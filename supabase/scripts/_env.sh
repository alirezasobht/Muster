# Sourced by the scripts beside it, never run on its own.
#
# Resolves the environment named in $1 to its Muster-env folder, prefix and
# project ref, then moves to the repo root so the Supabase CLI finds
# supabase/. Every command then passes --project-ref explicitly: the ref
# comes from the same folder as the values, so the two can't be mismatched.

case "${1:-}" in
  dev|prod) ;;
  *) echo "usage: $0 dev|prod" >&2; exit 1 ;;
esac

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
ENV_DIR="$REPO_ROOT/../Muster-env/$1"
PREFIX="$(echo "$1" | tr '[:lower:]' '[:upper:]')_"

[ -f "$ENV_DIR/env" ] || { echo "missing $ENV_DIR/env" >&2; exit 1; }
PROJECT_REF=$(grep "^${PREFIX}SUPABASE_PROJECT_REF=" "$ENV_DIR/env" | cut -d= -f2-)
[ -n "$PROJECT_REF" ] || { echo "no ${PREFIX}SUPABASE_PROJECT_REF in $ENV_DIR/env" >&2; exit 1; }

# Production takes a typed confirmation: every script here changes a live
# project, and prod is the one where a slip reaches real users. A wrapper
# that already confirmed (deploy-backend.sh) sets MUSTER_CONFIRMED_ENV so the
# scripts it runs don't ask again.
if [ "$1" = "prod" ] && [ "${MUSTER_CONFIRMED_ENV:-}" != "prod" ]; then
  printf "About to change PRODUCTION (%s). Type prod to continue: " "$PROJECT_REF"
  read -r answer
  [ "$answer" = "prod" ] || { echo "aborted" >&2; exit 1; }
fi

cd "$REPO_ROOT"
