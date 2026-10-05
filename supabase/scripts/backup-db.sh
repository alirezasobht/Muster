#!/bin/sh
# Dumps one environment's database into Muster-env/<env>/backups/<timestamp>/.
#
#   supabase/scripts/backup-db.sh dev
#   supabase/scripts/backup-db.sh prod
#
# Three files: roles, schema and data. db dump runs pg_dump in a container,
# so Docker or Podman must be installed; Podman is started and used when
# there is no docker command. The dump only reads, so prod asks no
# confirmation. Like push-migrations.sh, it relinks dev on the way out.
set -eu
MUSTER_CONFIRMED_ENV=${1:-}
. "$(dirname "$0")/_env.sh"

DEV_FILE="$REPO_ROOT/../Muster-env/dev/env"
DEV_REF=$(grep "^DEV_SUPABASE_PROJECT_REF=" "$DEV_FILE" | head -n 1 | cut -d= -f2- \
  | sed "s/^[\"']//; s/[\"']\$//")
[ -n "$DEV_REF" ] || { echo "no DEV_SUPABASE_PROJECT_REF in $DEV_FILE" >&2; exit 1; }

if ! command -v docker >/dev/null 2>&1; then
  command -v podman >/dev/null 2>&1 || { echo "install Docker Desktop or Podman first" >&2; exit 1; }
  podman machine start >/dev/null 2>&1 || true
  DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"
  export DOCKER_HOST
fi

relink_dev() {
  if supabase link --project-ref "$DEV_REF" >/dev/null; then
    echo "Relinked dev ($DEV_REF)"
  else
    echo "WARNING: relinking dev failed; the CLI may still be linked to $1" >&2
  fi
}
trap 'relink_dev "$1"' EXIT

OUT="$ENV_DIR/backups/$(date +%Y%m%d-%H%M)"
mkdir -p "$OUT"

echo "Linking $1 ($PROJECT_REF)"
supabase link --project-ref "$PROJECT_REF" >/dev/null
supabase db dump --linked --role-only -f "$OUT/roles.sql"
supabase db dump --linked -f "$OUT/schema.sql"
supabase db dump --linked --data-only --use-copy -f "$OUT/data.sql"

# A failed dump can still leave an empty file behind.
for f in roles schema data; do
  [ -s "$OUT/$f.sql" ] || { echo "empty $OUT/$f.sql: the dump failed" >&2; exit 1; }
done
echo "Backed up $1 to $OUT"
