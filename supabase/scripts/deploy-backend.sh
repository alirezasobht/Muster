#!/bin/sh
# Brings one environment's backend up to date: migrations, then Edge
# Function secrets, then the functions themselves.
#
#   supabase/scripts/deploy-backend.sh dev
#   supabase/scripts/deploy-backend.sh prod
#
# Schema first, so the functions deploy against the tables they read;
# secrets before functions, since a function missing a secret won't start.
# Stops at the first failure. Prod is confirmed once, here.
set -eu
. "$(dirname "$0")/_env.sh"

export MUSTER_CONFIRMED_ENV="$1"

"$SCRIPT_DIR/push-migrations.sh" "$1"
"$SCRIPT_DIR/push-secrets.sh" "$1"
"$SCRIPT_DIR/deploy-functions.sh" "$1"

echo "Backend for $1 ($PROJECT_REF) is up to date"
