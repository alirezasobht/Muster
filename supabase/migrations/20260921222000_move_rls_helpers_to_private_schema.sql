-- Move RLS-only helper functions out of the exposed public schema.
--
-- authenticated still needs EXECUTE on these functions because RLS
-- policies call them, but they will no longer be exposed as public RPCs.

create schema if not exists private;

revoke all on schema private from public, anon;
grant usage on schema private to authenticated;


-- ============================================================
-- Move the six RLS helper functions.
--
-- ALTER FUNCTION preserves their OIDs, so existing RLS policy
-- dependencies continue pointing to the same functions.
-- ============================================================

alter function public.can_create_groups()
set schema private;

alter function public.group_is_live(uuid)
set schema private;

alter function public.has_pending_invitation(uuid)
set schema private;

alter function public.is_group_admin(uuid)
set schema private;

alter function public.is_group_member(uuid)
set schema private;

alter function public.my_email()
set schema private;


-- Explicitly keep them unavailable to anon/PUBLIC and usable by
-- authenticated for RLS evaluation.

revoke execute on function private.can_create_groups()
from public, anon;

grant execute on function private.can_create_groups()
to authenticated;


revoke execute on function private.group_is_live(uuid)
from public, anon;

grant execute on function private.group_is_live(uuid)
to authenticated;


revoke execute on function private.has_pending_invitation(uuid)
from public, anon;

grant execute on function private.has_pending_invitation(uuid)
to authenticated;


revoke execute on function private.is_group_admin(uuid)
from public, anon;

grant execute on function private.is_group_admin(uuid)
to authenticated;


revoke execute on function private.is_group_member(uuid)
from public, anon;

grant execute on function private.is_group_member(uuid)
to authenticated;


revoke execute on function private.my_email()
from public, anon;

grant execute on function private.my_email()
to authenticated;


-- ============================================================
-- Two public client-facing RPCs call moved helpers internally.
-- Update those calls to use the private schema explicitly.
-- ============================================================

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
set search_path = 'public'
as $$
    select
        gi.id,
        gi.group_id,
        g.name,
        p.name
    from public.group_invitations gi
    join public.groups g
      on g.id = gi.group_id
    join public.profiles p
      on p.id = gi.invited_by
    where gi.status = 'pending'
      and g.archived_at is null
      and gi.email = private.my_email();
$$;


create or replace function public.set_standby_order(
    eid uuid,
    ordered_players uuid[]
)
returns void
language plpgsql
security definer
set search_path = 'public'
as $$
declare
    ev  public.events;
    gid uuid;
    pid uuid;
    i   int := 0;
begin
    select group_id
    into gid
    from public.events
    where id = eid;

    if not found then
        raise exception 'no such event';
    end if;

    perform 1
    from public.groups
    where id = gid
      and archived_at is null
    for share;

    if not found then
        raise exception 'group is archived';
    end if;

    select *
    into ev
    from public.events
    where id = eid
    for update;

    if not found then
        raise exception 'no such event';
    end if;

    if not private.is_group_admin(ev.group_id) then
        raise exception 'only an admin can change the standby queue';
    end if;

    if ev.starts_at <= clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if coalesce(array_length(ordered_players, 1), 0) is distinct from
       (
           select count(distinct x)
           from unnest(ordered_players) x
       )
    then
        raise exception 'duplicate player in queue';
    end if;

    foreach pid in array coalesce(ordered_players, '{}'::uuid[])
    loop
        if not exists (
            select 1
            from public.group_members
            where group_id = ev.group_id
              and profile_id = pid
        ) then
            raise exception
                'player % is not a member of this group',
                coalesce(
                    (
                        select name
                        from public.profiles
                        where id = pid
                    ),
                    pid::text
                );
        end if;

        if exists (
            select 1
            from public.event_invitations
            where event_id = eid
              and profile_id = pid
        ) then
            raise exception 'queue is out of date, reload';
        end if;
    end loop;

    delete from public.event_standby
    where event_id = eid
      and profile_id <> all (
          coalesce(ordered_players, '{}'::uuid[])
      );

    foreach pid in array coalesce(ordered_players, '{}'::uuid[])
    loop
        i := i + 1;

        insert into public.event_standby (
            event_id,
            group_id,
            profile_id,
            position
        )
        values (
            eid,
            ev.group_id,
            pid,
            i
        )
        on conflict (event_id, profile_id)
        do update
        set position = excluded.position;
    end loop;
end;
$$;


-- Reassert client RPC permissions because CREATE OR REPLACE should
-- preserve them, but keeping them explicit makes the migration clear.

revoke execute on function public.get_my_pending_invitations()
from public, anon;

grant execute on function public.get_my_pending_invitations()
to authenticated;


revoke execute on function public.set_standby_order(uuid, uuid[])
from public, anon;

grant execute on function public.set_standby_order(uuid, uuid[])
to authenticated;