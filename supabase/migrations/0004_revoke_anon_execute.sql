-- Close anon's EXECUTE on every function in public.
--
-- The earlier migrations used `revoke execute ... from public`, which is not enough on
-- Supabase: default privileges grant EXECUTE to `anon`, `authenticated` and `service_role`
-- as *named roles*, and revoking from PUBLIC does not remove a named-role grant. The result
-- was that every function, including SECURITY DEFINER ones, stayed callable without signing
-- in via /rest/v1/rpc/<name>.
--
-- Most were harmless because they check auth.uid() internally, but record_author() returned
-- the author of any record to an anonymous caller who knew its id. Fixed here, and fixed
-- again inside the function itself so it does not depend solely on a grant.

-- Nothing in this schema should be reachable without signing in.
-- Both PUBLIC and the named role have to be revoked: they are separate grants, and a
-- PUBLIC grant alone still lets every role through.
revoke execute on all functions in schema public from public, anon;

-- Trigger functions are never meant to be called directly by anyone.
revoke execute on function public.touch_updated_at() from public, anon, authenticated;
revoke execute on function public.enforce_workspace_member_cap()
    from public, anon, authenticated;

-- Future functions in this schema default to no anon access, so a new migration cannot
-- silently reintroduce this.
alter default privileges in schema public revoke execute on functions from anon;

-- A SECURITY DEFINER function should not rely only on its grants. Both helpers are used
-- inside RLS policies, so `authenticated` must keep EXECUTE; the guard makes an unauthenticated
-- call fail even if a grant is ever restored by accident.
create or replace function public.record_author(p_id uuid)
returns uuid
language plpgsql
stable
security definer
set search_path = public, pg_catalog
as $$
begin
    if auth.uid() is null then
        raise exception 'not authenticated' using errcode = 'insufficient_privilege';
    end if;

    return (select created_by from public.records where id = p_id);
end;
$$;

revoke execute on function public.record_author(uuid) from public, anon;
grant execute on function public.record_author(uuid) to authenticated;

create or replace function public.is_workspace_member(ws uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_catalog
as $$
    select exists (
        select 1
        from public.workspace_members m
        where m.workspace_id = ws
          and m.user_id = auth.uid()
    );
$$;

revoke execute on function public.is_workspace_member(uuid) from public, anon;
grant execute on function public.is_workspace_member(uuid) to authenticated;

-- Flagged by the linter: a trigger function without a pinned search_path can be hijacked by
-- a caller-controlled search_path.
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = public, pg_catalog
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

revoke execute on function public.touch_updated_at() from public, anon, authenticated;
