# Source it, don't run it: exports only reach the current shell that way.
#
#   . scripts/set-env-vars.sh dev
#   . scripts/set-env-vars.sh prod
#
# Points the app build at one environment by exporting SUPABASE_URL and
# SUPABASE_PUBLISHABLE_KEY, which the build prefers over local.properties.
# Run ./gradlew from the same shell; Android Studio's Run button won't see
# them. return, never exit, and no set -e: either would reach the caller's
# shell.

case "${1:-}" in
  dev|prod) ;;
  *) echo "usage: . scripts/set-env-vars.sh dev|prod" >&2; return 1 ;;
esac

_root=$(git rev-parse --show-toplevel) || return 1
_file="$_root/../Muster-env/$1/env"
_prefix="$(echo "$1" | tr '[:lower:]' '[:upper:]')_"

if [ ! -f "$_file" ]; then
  echo "missing $_file" >&2
else
  _url=$(grep "^${_prefix}SUPABASE_URL=" "$_file" | cut -d= -f2-)
  _key=$(grep "^${_prefix}SUPABASE_PUBLISHABLE_KEY=" "$_file" | cut -d= -f2-)
  if [ -z "$_url" ] || [ -z "$_key" ]; then
    echo "missing ${_prefix}SUPABASE_URL or ${_prefix}SUPABASE_PUBLISHABLE_KEY in $_file" >&2
  else
    export SUPABASE_URL="$_url" SUPABASE_PUBLISHABLE_KEY="$_key"
    echo "App build now targets $1 ($SUPABASE_URL)"
  fi
fi
unset _root _file _prefix _url _key
