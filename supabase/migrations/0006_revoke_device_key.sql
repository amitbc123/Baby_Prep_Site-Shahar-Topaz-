-- Device key revocation.
--
-- Scope, deliberately narrow: this lets a workspace member mark a device_keys row as
-- revoked so it stops being offered as a pairing target for *future* key-wrap grants (see
-- PairingViewModel's pending-device list). It does NOT rotate the workspace's symmetric
-- key, and it does NOT end that device's Supabase Auth session — an already-synced device
-- keeps reading/writing `records` until its session expires or is signed out elsewhere,
-- because `records` RLS is governed by workspace_members/auth.uid(), not device_keys.
-- Full workspace-key rotation (re-encrypt all records under a new key, re-wrap to every
-- remaining device) is out of scope here: it needs pgTAP coverage of the re-wrap fan-out
-- that this environment cannot currently run.
--
-- A SECURITY DEFINER RPC is used instead of widening the device_keys UPDATE policy,
-- because a workspace-member-scoped UPDATE policy on device_keys would let either partner
-- overwrite the OTHER partner's device's public_key column too, not just revoked_at —
-- that's a MITM vector, not a revoke feature. The RPC only ever touches revoked_at.

create or replace function public.revoke_device_key(target_device_key_id uuid)
returns void
language plpgsql
security definer
set search_path = public, pg_catalog
as $$
declare
    target public.device_keys;
begin
    if auth.uid() is null then
        raise exception 'not authenticated' using errcode = 'insufficient_privilege';
    end if;

    select * into target from public.device_keys where id = target_device_key_id;

    if target.id is null or not public.is_workspace_member(target.workspace_id) then
        raise exception 'device not found' using errcode = 'invalid_parameter_value';
    end if;

    update public.device_keys
       set revoked_at = now()
     where id = target_device_key_id
       and revoked_at is null;
end;
$$;

revoke execute on function public.revoke_device_key(uuid) from public;
grant execute on function public.revoke_device_key(uuid) to authenticated;
