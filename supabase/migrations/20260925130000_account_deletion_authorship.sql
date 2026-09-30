-- Account deletion, step 1: authorship no longer blocks deleting a
-- profile, and a sole admin's groups can be archived out of the way.

alter table public.groups
    alter column created_by drop not null,
    drop constraint groups_created_by_fkey,
    add constraint groups_created_by_fkey
        foreign key (created_by) references public.profiles (id) on delete set null;

alter table public.events
    alter column created_by drop not null,
    drop constraint events_created_by_fkey,
    add constraint events_created_by_fkey
        foreign key (created_by) references public.profiles (id) on delete set null;

alter table public.group_invitations
    alter column invited_by drop not null,
    drop constraint group_invitations_invited_by_fkey,
    add constraint group_invitations_invited_by_fkey
        foreign key (invited_by) references public.profiles (id) on delete set null;

-- Archived groups are skipped so deleting a sole admin's membership can
-- pass. A group restored from the dashboard needs an admin set by hand.
create or replace function public.ensure_admin_remains()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid := coalesce(new.group_id, old.group_id);
    archived timestamptz;
begin
    -- FOR UPDATE serialises membership changes within a group: the check
    -- is deferred, so without it two concurrent demotions could each see
    -- the other's uncommitted admin row and leave the group with none.
    select g.archived_at into archived
    from public.groups g
    where g.id = gid
    for update;

    if not found or archived is not null then
        return null;
    end if;

    if not exists (
        select 1 from public.group_members gm
        where gm.group_id = gid and gm.role = 'admin'
    ) then
        raise exception 'a group must keep at least one admin';
    end if;

    return null;
end;
$$;

-- Deleting a profile nulls events.created_by, including on past events,
-- which the freeze would otherwise reject.
create or replace function public.reject_if_event_started_self()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    unchanged public.events := old;
begin
    unchanged.created_by := new.created_by;
    if unchanged is not distinct from new then
        return new;
    end if;

    if old.starts_at <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    return new;
end;
$$;

-- Left join, so an invitation from a deleted admin still shows. The
-- client already falls back when inviter_name is null.
create or replace function public.get_my_pending_invitations()
returns table(
    invitation_id uuid,
    group_id uuid,
    group_name text,
    inviter_name text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        gi.id,
        gi.group_id,
        g.name,
        p.name
    from public.group_invitations gi
    join public.groups g
      on g.id = gi.group_id
    left join public.profiles p
      on p.id = gi.invited_by
    where gi.status = 'pending'
      and g.archived_at is null
      and gi.email = private.my_email();
$$;

revoke execute on function public.get_my_pending_invitations() from public, anon;
grant execute on function public.get_my_pending_invitations() to authenticated;
