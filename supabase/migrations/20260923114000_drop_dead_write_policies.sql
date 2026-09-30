-- Every client write grant is revoked, so these policies can never apply.
-- Definer RPCs and triggers own all writes; only SELECT policies remain.

drop policy profiles_update on public.profiles;

drop policy groups_insert on public.groups;
drop policy groups_update on public.groups;

drop policy group_members_update on public.group_members;
drop policy group_members_delete on public.group_members;

drop policy group_invitations_insert on public.group_invitations;
drop policy group_invitations_delete on public.group_invitations;

drop policy events_insert on public.events;
drop policy events_update on public.events;
drop policy events_delete on public.events;

drop policy event_invitations_insert on public.event_invitations;
drop policy event_invitations_update on public.event_invitations;
drop policy event_invitations_delete on public.event_invitations;
