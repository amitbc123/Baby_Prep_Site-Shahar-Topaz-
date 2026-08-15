-- Atomic push with optimistic concurrency.
--
-- Doing this as read-then-write from the client would race: both devices read version 3,
-- both write version 4, and one edit is lost with no conflict ever reported. The version
-- check and the write have to happen in one statement, on the server, so exactly one of two
-- concurrent writers wins and the other is told it conflicted.
--
-- Ciphertext crosses as base64 because JSON has no byte type. The server decodes it to bytea
-- and stores it; it still cannot read what is inside.

create or replace function public.push_records(p_workspace uuid, p_records jsonb)
returns jsonb
language plpgsql
security invoker
set search_path = public, pg_catalog
as $$
declare
    item        jsonb;
    results     jsonb := '[]'::jsonb;
    existing    public.records;
    new_version integer;
    incoming_id uuid;
begin
    if not public.is_workspace_member(p_workspace) then
        raise exception 'not a member of this workspace' using errcode = 'insufficient_privilege';
    end if;

    for item in select * from jsonb_array_elements(p_records)
    loop
        incoming_id := (item ->> 'id')::uuid;

        select * into existing from public.records where id = incoming_id for update;

        -- A retry after a lost response: the write already landed, so report it as applied
        -- rather than as a conflict against our own earlier self.
        if existing.id is not null
           and item ->> 'client_mutation_id' is not null
           and existing.client_mutation_id is not distinct from (item ->> 'client_mutation_id')::uuid
        then
            results := results || jsonb_build_object(
                'id', existing.id,
                'status', 'applied',
                'version', existing.version
            );
            continue;
        end if;

        if existing.id is null then
            insert into public.records (
                id, workspace_id, entity_type, ciphertext, version,
                client_mutation_id, created_by, deleted_at
            )
            values (
                incoming_id,
                p_workspace,
                (item ->> 'entity_type')::public.entity_type,
                decode(item ->> 'ciphertext', 'base64'),
                1,
                (item ->> 'client_mutation_id')::uuid,
                auth.uid(),
                case when (item ->> 'deleted')::boolean then now() else null end
            );

            results := results || jsonb_build_object('id', incoming_id, 'status', 'applied', 'version', 1);

        elsif existing.version = (item ->> 'base_version')::integer then
            new_version := existing.version + 1;

            update public.records
               set ciphertext = decode(item ->> 'ciphertext', 'base64'),
                   version = new_version,
                   client_mutation_id = (item ->> 'client_mutation_id')::uuid,
                   deleted_at = case
                       when (item ->> 'deleted')::boolean then coalesce(existing.deleted_at, now())
                       else null
                   end
             where id = incoming_id;

            results := results || jsonb_build_object('id', incoming_id, 'status', 'applied', 'version', new_version);

        else
            -- Stale write. Hand back the server's row so the client can show both sides;
            -- the server never picks a winner.
            results := results || jsonb_build_object(
                'id', existing.id,
                'status', 'conflict',
                'version', existing.version,
                'entity_type', existing.entity_type,
                'ciphertext', encode(existing.ciphertext, 'base64'),
                'updated_at', extract(epoch from existing.updated_at) * 1000,
                'deleted_at', case
                    when existing.deleted_at is null then null
                    else extract(epoch from existing.deleted_at) * 1000
                end
            );
        end if;
    end loop;

    return results;
end;
$$;

revoke execute on function public.push_records(uuid, jsonb) from public;
grant execute on function public.push_records(uuid, jsonb) to authenticated;

-- Paging read for the sync cursor. A plain PostgREST select could do this, but going through
-- a function keeps the epoch-millis conversion in one place and matches the push shape.
create or replace function public.pull_records(p_workspace uuid, p_since bigint, p_limit integer)
returns jsonb
language plpgsql
security invoker
stable
set search_path = public, pg_catalog
as $$
declare
    result jsonb;
begin
    if not public.is_workspace_member(p_workspace) then
        raise exception 'not a member of this workspace' using errcode = 'insufficient_privilege';
    end if;

    select coalesce(jsonb_agg(row_to_json(r)::jsonb order by r.updated_at), '[]'::jsonb)
      into result
      from (
        select id,
               entity_type,
               encode(ciphertext, 'base64') as ciphertext,
               version,
               (extract(epoch from updated_at) * 1000)::bigint as updated_at,
               case
                   when deleted_at is null then null
                   else (extract(epoch from deleted_at) * 1000)::bigint
               end as deleted_at
          from public.records
         where workspace_id = p_workspace
           and (p_since is null or extract(epoch from updated_at) * 1000 > p_since)
         order by updated_at
         limit least(coalesce(p_limit, 200), 500)
      ) r;

    return result;
end;
$$;

revoke execute on function public.pull_records(uuid, bigint, integer) from public;
grant execute on function public.pull_records(uuid, bigint, integer) to authenticated;
