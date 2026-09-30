#!/bin/sh
# Applies pending migrations to one environment's Supabase project.
#
#   supabase/scripts/push-migrations.sh dev
#   supabase/scripts/push-migrations.sh prod
#
# db push only works on the linked project, so this links the environment,
# pushes, and relinks dev on the way out, failure included: the link is saved
# in the repo, and every terminal's supabase commands follow it. No database
# password: the CLI signs in with its own temporary login role.
set -eu
. "$(dirname "$0")/_env.sh"

DEV_FILE="$REPO_ROOT/../Muster-env/dev/env"
DEV_REF=$(grep "^DEV_SUPABASE_PROJECT_REF=" "$DEV_FILE" | head -n 1 | cut -d= -f2- \
  | sed "s/^[\"']//; s/[\"']\$//")
[ -n "$DEV_REF" ] || { echo "no DEV_SUPABASE_PROJECT_REF in $DEV_FILE" >&2; exit 1; }

relink_dev() {
  if supabase link --project-ref "$DEV_REF" >/dev/null; then
    echo "Relinked dev ($DEV_REF)"
  else
    echo "WARNING: relinking dev failed; the CLI may still be linked to $1" >&2
  fi
}
trap 'relink_dev "$1"' EXIT

echo "Linking $1 ($PROJECT_REF)"
supabase link --project-ref "$PROJECT_REF" >/dev/null
echo "Pushing migrations to $1"
supabase db push
