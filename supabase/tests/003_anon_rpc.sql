-- Anonymous callers must not be able to invoke anything in the public schema.
--
-- This file exists because of a real miss: migrations 0001-0003 used
-- `revoke execute ... from public`, which does not remove Supabase's default named-role grant
-- to `anon`. Every RPC stayed callable without signing in, and no test noticed because none
-- of them ever acted as `anon`. These do.

begin;

create extension if not exists pgtap with schema extensions;

select plan(8);

create schema if not exists tests;

create or replace function tests.act_as_anon()
returns void language plpgsql as $$
begin
    perform set_config('role', 'anon', true);
    perform set_config('request.jwt.claims', json_build_object('role', 'anon')::text, true);
end;
$$;

grant usage on schema tests to anon;
grant execute on all functions in schema tests to anon;

-- ---------------------------------------------------------------------------
-- No function in public is executable by anon
-- ---------------------------------------------------------------------------

select is(
    (select count(*)::int
       from pg_proc p
       join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public'
        and has_function_privilege('anon', p.oid, 'EXECUTE')),
    0,
    'anon holds EXECUTE on no function in the public schema'
);

select is(
    (select count(*)::int
       from pg_proc p
       join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public'
        and p.prosecdef
        and has_function_privilege('anon', p.oid, 'EXECUTE')),
    0,
    'anon holds EXECUTE on no SECURITY DEFINER function'
);

select is(
    (select count(*)::int
       from pg_proc p
       join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public'
        and p.proname in ('touch_updated_at', 'enforce_workspace_member_cap')
        and has_function_privilege('authenticated', p.oid, 'EXECUTE')),
    0,
    'trigger functions are not directly callable by signed-in users either'
);

-- ---------------------------------------------------------------------------
-- Every function pins its search_path
-- ---------------------------------------------------------------------------

select is(
    (select count(*)::int
       from pg_proc p
       join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public'
        and p.proconfig is null),
    0,
    'every function in public pins a search_path'
);

-- ---------------------------------------------------------------------------
-- Anonymous calls are refused in practice, not just on paper
-- ---------------------------------------------------------------------------

select tests.act_as_anon();

select throws_ok(
    $$ select public.create_workspace() $$,
    '42501',
    null,
    'anon cannot create a workspace'
);

select throws_ok(
    $$ select public.accept_invitation('anything') $$,
    '42501',
    null,
    'anon cannot redeem an invitation'
);

select throws_ok(
    $$ select public.record_author('22222222-2222-2222-2222-222222222222'::uuid) $$,
    '42501',
    null,
    'anon cannot look up who authored a record'
);

select throws_ok(
    $$ select public.push_records(gen_random_uuid(), '[]'::jsonb) $$,
    '42501',
    null,
    'anon cannot push records'
);

select * from finish();

rollback;
