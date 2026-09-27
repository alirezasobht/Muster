#!/bin/sh
# Deploys prod, then resets everything to dev.
#
#   scripts/deploy-prod.sh
#
# Run it, don't source it: set -e would close your shell on the first error.
# The upload to Cloudflare Pages stays manual; the folder is printed at the end.
set -eu

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"

DEV_FILE="$ROOT/../Muster-env/dev/env"
DEV_REF=$(grep "^DEV_SUPABASE_PROJECT_REF=" "$DEV_FILE" | head -n 1 | cut -d= -f2- \
  | sed "s/^[\"']//; s/[\"']\$//")

# Reset to dev on the way out, failure included.
reset_to_dev() {
  supabase link --project-ref "$DEV_REF" >/dev/null \
    && echo "Supabase CLI linked to dev ($DEV_REF)"
  . scripts/set-env-vars.sh dev
}
trap reset_to_dev EXIT

supabase/scripts/deploy-backend.sh prod
. scripts/set-env-vars.sh prod
./gradlew :webApp:clean :webApp:composeCompatibilityBrowserDistribution

echo
echo "Upload to the prod Pages project:"
echo "  $ROOT/webApp/build/dist/composeWebCompatibility/productionExecutable"
