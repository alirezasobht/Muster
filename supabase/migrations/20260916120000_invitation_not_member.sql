-- Muster: reject inviting someone who is already in the group.
--
-- The partial unique index on (group_id, email) where status = 'pending'
-- already stops a second open invitation to the same address (23505). It
-- says nothing about someone who has already accepted: the invitation
-- inserts fine, and the failure surfaces much later when
-- accept_group_invitation trips the group_members primary key. By then the
-- admin has been told the invite was sent.
--
-- Checking in the client instead would be racy — two admins inviting at
-- once, or the invitee accepting between the check and the insert.
--
-- security definer because the admin cannot read the invitee's profiles
-- row: profiles_select requires a shared group, which is exactly what is
-- being established. The function reads only enough to answer the
-- question and returns no data.
create or replace function reject_if_already_member()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if exists (
    select 1
    from profiles p
    join group_members gm
      on gm.profile_id = p.id
     and gm.group_id = new.group_id
    where p.email = new.email
  ) then
-- The message is the contract: the mapper matches on it, the way it already
-- does for the invitation RPCs and group_keeps_an_admin. Changing this
-- wording breaks the client's error.
    raise exception 'already a member of this group';
  end if;
  return new;
end;
$$;

create trigger group_invitations_not_member
before insert on group_invitations
for each row
execute function reject_if_already_member();
