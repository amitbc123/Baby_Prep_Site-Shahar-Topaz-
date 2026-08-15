-- Stop a member from rewriting attribution on an existing record.
--
-- The previous records_update policy checked membership on both sides but said nothing about
-- created_by, so either member could reassign authorship of the other's record. It is only
-- attribution, not access -- both members can read and edit everything by design -- but "who
-- wrote this" should not be silently rewritable, and the fix costs nothing.
--
-- Note the WITH CHECK must reference the *new* row while USING references the old one; a
-- policy with only USING would let the new row take any shape it liked.

create or replace function public.record_author(p_id uuid)
returns uuid
language sql
stable
security definer
set search_path = public, pg_catalog
as $$
    select created_by from public.records where id = p_id;
$$;

revoke execute on function public.record_author(uuid) from public;
grant execute on function public.record_author(uuid) to authenticated;

drop policy if exists records_update on public.records;

create policy records_update on public.records
    for update to authenticated
    using (public.is_workspace_member(workspace_id))
    with check (
        public.is_workspace_member(workspace_id)
        and created_by = public.record_author(id)
    );
