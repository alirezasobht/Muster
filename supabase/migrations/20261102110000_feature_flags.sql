-- Remote feature flags, edited in the dashboard. Each platform column is
-- the first app version the flag is on for. Null means off on that
-- platform. Only the app reads flags; no RPC or trigger uses them.

create table private.feature_flags (
    flag text primary key check (flag ~ '^[a-z][a-z0-9_]*$'),
    android text check (android ~ '^\d+\.\d+\.\d+$'),
    ios text check (ios ~ '^\d+\.\d+\.\d+$'),
    web text check (web ~ '^\d+\.\d+\.\d+$')
);

revoke all on table private.feature_flags from public, anon, authenticated;


-- Returns every flag, on or off, so the app can replace all its saved
-- flags. Granted to anon because the app syncs at launch, before anyone
-- signs in. An unknown platform gets the web column.
create function public.get_feature_flags(platform text, version text)
returns table(flag text, enabled boolean)
language plpgsql
security definer
stable
set search_path = ''
as $$
declare
    app_version int[];
begin
    if get_feature_flags.version is null
        or get_feature_flags.version !~ '^\d+\.\d+\.\d+$' then
        raise exception 'version must be major.minor.patch';
    end if;

    app_version := pg_catalog.string_to_array(get_feature_flags.version, '.')::int[];

    return query
    select f.flag,
        coalesce(
            pg_catalog.string_to_array(
                case get_feature_flags.platform
                    when 'android' then f.android
                    when 'ios' then f.ios
                    else f.web
                end,
                '.'
            )::int[] <= app_version,
            false
        )
    from private.feature_flags f;
end;
$$;

revoke all on function public.get_feature_flags(text, text) from public, anon, authenticated;
grant execute on function public.get_feature_flags(text, text) to anon, authenticated;
