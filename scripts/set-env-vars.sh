# Source it, don't run it: exports only reach the current shell that way.
#
#   . scripts/set-env-vars.sh dev
#   . scripts/set-env-vars.sh prod
#
# Points the app build at one environment by exporting SUPABASE_URL and
# SUPABASE_PUBLISHABLE_KEY from env, and CONTACT_EMAIL and WEB_APP_URL from
# edge.env, which the build prefers over local.properties. Run ./gradlew from
# the same shell; Android Studio's Run button won't see them. return, never
# exit, and no set -e: either would reach the caller's shell.

case "${1:-}" in
  dev|prod) ;;
  *) echo "usage: . scripts/set-env-vars.sh dev|prod" >&2; return 1 ;;
esac

_root=$(git rev-parse --show-toplevel) || return 1
_dir="$_root/../Muster-env/$1"
_prefix="$(echo "$1" | tr '[:lower:]' '[:upper:]')_"

if [ ! -f "$_dir/env" ] || [ ! -f "$_dir/edge.env" ]; then
  echo "missing $_dir/env or $_dir/edge.env" >&2
else
  # Values may be quoted in the files; the quotes must not reach the build.
  _read() {
    grep "^${_prefix}$1=" "$_dir/$2" | cut -d= -f2- | sed -e 's/^["'\'']//' -e 's/["'\'']$//'
  }
  _url=$(_read SUPABASE_URL env)
  _key=$(_read SUPABASE_PUBLISHABLE_KEY env)
  _contact=$(_read CONTACT_EMAIL edge.env)
  _web=$(_read WEB_APP_URL edge.env)
  if [ -z "$_url" ] || [ -z "$_key" ] || [ -z "$_contact" ] || [ -z "$_web" ]; then
    echo "missing one of ${_prefix}SUPABASE_URL, ${_prefix}SUPABASE_PUBLISHABLE_KEY (env)," \
      "${_prefix}CONTACT_EMAIL, ${_prefix}WEB_APP_URL (edge.env)" >&2
  else
    export SUPABASE_URL="$_url" SUPABASE_PUBLISHABLE_KEY="$_key" \
      CONTACT_EMAIL="$_contact" WEB_APP_URL="$_web"
    echo "App build now targets $1 ($SUPABASE_URL)"
  fi
fi
unset _root _dir _prefix _url _key _contact _web
unset -f _read 2>/dev/null
