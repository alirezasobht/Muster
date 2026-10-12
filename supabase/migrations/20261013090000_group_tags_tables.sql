-- Member tags: admins sort members into tags such as Regular or Casual.
-- A member has at most one tag, and only admins read either table. Every
-- write goes through the tag RPCs in the next migrations.

create table public.group_tags (
    id         uuid primary key default gen_random_uuid(),
    group_id   uuid not null references public.groups (id) on delete cascade,
    name       text not null,
    position   int  not null check (position > 0),
    created_at timestamptz not null default now(),

    -- The app maps these names to its own messages; renaming one breaks that.
    constraint group_tags_name_trimmed   check (name = btrim(name)),
    constraint group_tags_name_not_blank check (char_length(name) > 0),
    constraint group_tags_name_length    check (char_length(name) <= 30),

    -- Lets group_member_tags reference the pair, so a tag can only hold
    -- members of its own group.
    unique (id, group_id)
);

create unique index group_tags_name_unique
    on public.group_tags (group_id, lower(name));

-- Deferrable so a reorder can renumber rows within one transaction.
create unique index group_tags_position
    on public.group_tags (group_id, position);
alter table public.group_tags
    add constraint group_tags_position_unique
    unique using index group_tags_position deferrable initially deferred;

create table public.group_member_tags (
    group_id   uuid not null,
    profile_id uuid not null,
    tag_id     uuid not null,
    position   int  not null check (position > 0),

    -- One tag per member.
    primary key (group_id, profile_id),

    -- Leaving or being removed drops the tag.
    foreign key (group_id, profile_id)
        references public.group_members (group_id, profile_id) on delete cascade,

    -- Deleting a tag untags its members.
    foreign key (tag_id, group_id)
        references public.group_tags (id, group_id) on delete cascade
);

create unique index group_member_tags_position
    on public.group_member_tags (tag_id, position);
alter table public.group_member_tags
    add constraint group_member_tags_position_unique
    unique using index group_member_tags_position deferrable initially deferred;

alter table public.group_tags        enable row level security;
alter table public.group_member_tags enable row level security;

create policy group_tags_select on public.group_tags
for select to authenticated
using (private.is_group_admin(group_id));

create policy group_member_tags_select on public.group_member_tags
for select to authenticated
using (private.is_group_admin(group_id));

-- Supabase still grants writes on new tables by default until 2026-10-30.
revoke all on public.group_tags        from public, anon, authenticated;
revoke all on public.group_member_tags from public, anon, authenticated;
grant select on public.group_tags        to authenticated;
grant select on public.group_member_tags to authenticated;
