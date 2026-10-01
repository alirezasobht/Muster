#!/bin/sh
# Deploys prod, then resets everything to dev.
#
#   scripts/deploy-prod.sh
#
# Run it, don't source it: set -e would close your shell on the first error.
# The uploads to Cloudflare Pages and Play stay manual; the paths are printed
# at the end.
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

PROD_DIR=$(cd "$ROOT/../Muster-env/prod" && pwd)
export UPLOAD_KEYSTORE_PATH="$PROD_DIR/upload-keystore.jks"
export UPLOAD_KEYSTORE_PASSWORD=$(grep "^PROD_UPLOAD_KEYSTORE_PASSWORD=" "$PROD_DIR/env" \
  | head -n 1 | cut -d= -f2- | sed "s/^[\"']//; s/[\"']\$//")
# An unsigned bundle builds fine and only fails at the Play upload.
[ -f "$UPLOAD_KEYSTORE_PATH" ] || { echo "missing $UPLOAD_KEYSTORE_PATH" >&2; exit 1; }
[ -n "$UPLOAD_KEYSTORE_PASSWORD" ] || { echo "no PROD_UPLOAD_KEYSTORE_PASSWORD in $PROD_DIR/env" >&2; exit 1; }

./gradlew :webApp:clean :webApp:composeCompatibilityBrowserDistribution
./gradlew :androidApp:clean :androidApp:bundleRelease

echo
echo "Upload to the prod Pages project:"
echo "  $ROOT/webApp/build/dist/composeWebCompatibility/productionExecutable"
echo "Upload to Play Console:"
echo "  $ROOT/androidApp/build/outputs/bundle/release/androidApp-release.aab"
