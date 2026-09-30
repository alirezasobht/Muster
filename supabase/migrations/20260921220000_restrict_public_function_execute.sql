-- Restrict EXECUTE permissions on public functions.
--
-- Rules:
--   1. anon cannot execute any application/database function.
--   2. authenticated can execute only:
--        - client-facing RPCs
--        - helper functions required by RLS policies
--   3. trigger/internal functions cannot be called directly by clients.
--
-- PUBLIC is also revoked because PostgreSQL grants function EXECUTE to
-- PUBLIC by default, which would otherwise indirectly give access to
-- anon/authenticated.

-- ============================================================
-- Client-facing RPCs
-- ============================================================

revoke execute on function public.accept_group_invitation(uuid)
from public, anon;

grant execute on function public.accept_group_invitation(uuid)
to authenticated;


revoke execute on function public.decline_group_invitation(uuid)
from public, anon;

grant execute on function public.decline_group_invitation(uuid)
to authenticated;


revoke execute on function public.get_my_pending_invitations()
from public, anon;

grant execute on function public.get_my_pending_invitations()
to authenticated;


revoke execute on function public.set_standby_order(uuid, uuid[])
from public, anon;

grant execute on function public.set_standby_order(uuid, uuid[])
to authenticated;


revoke execute on function public.disinvite_player(uuid, uuid)
from public, anon;

grant execute on function public.disinvite_player(uuid, uuid)
to authenticated;


-- ============================================================
-- RLS / authorization helpers
--
-- These are not called directly by the app, but current RLS
-- policies execute them as the authenticated role.
-- ============================================================

revoke execute on function public.can_create_groups()
from public, anon;

grant execute on function public.can_create_groups()
to authenticated;


revoke execute on function public.group_is_live(uuid)
from public, anon;

grant execute on function public.group_is_live(uuid)
to authenticated;


revoke execute on function public.has_pending_invitation(uuid)
from public, anon;

grant execute on function public.has_pending_invitation(uuid)
to authenticated;


revoke execute on function public.is_group_admin(uuid)
from public, anon;

grant execute on function public.is_group_admin(uuid)
to authenticated;


revoke execute on function public.is_group_member(uuid)
from public, anon;

grant execute on function public.is_group_member(uuid)
to authenticated;


revoke execute on function public.my_email()
from public, anon;

grant execute on function public.my_email()
to authenticated;


-- ============================================================
-- Internal / trigger functions
--
-- These must not be callable directly through PostgREST/RPC.
-- Their triggers and SECURITY DEFINER callers can still invoke
-- them internally.
-- ============================================================

revoke execute on function public.promote_standby(uuid)
from public, anon, authenticated;

revoke execute on function public.enforce_capacity()
from public, anon, authenticated;

revoke execute on function public.ensure_admin_remains()
from public, anon, authenticated;

revoke execute on function public.handle_new_group()
from public, anon, authenticated;

revoke execute on function public.handle_new_user()
from public, anon, authenticated;

revoke execute on function public.reject_if_already_member()
from public, anon, authenticated;

revoke execute on function public.reject_if_event_started()
from public, anon, authenticated;

revoke execute on function public.reject_if_event_started_self()
from public, anon, authenticated;

revoke execute on function public.reject_if_invited()
from public, anon, authenticated;

revoke execute on function public.reject_if_queued()
from public, anon, authenticated;

revoke execute on function public.restore_profile_on_sign_in()
from public, anon, authenticated;

revoke execute on function public.rls_auto_enable()
from public, anon, authenticated;

revoke execute on function public.sync_user_email()
from public, anon, authenticated;

revoke execute on function public.trigger_promote_standby()
from public, anon, authenticated;