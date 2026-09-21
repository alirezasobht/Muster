#!/bin/sh
# Deploys every Edge Function to one environment's Supabase project.
#
#   supabase/scripts/deploy-functions.sh dev
#   supabase/scripts/deploy-functions.sh prod
#
# Per-function settings, verify_jwt included, come from supabase/config.toml.
# Push secrets first: a function whose secrets are missing refuses to start.
set -eu
. "$(dirname "$0")/_env.sh"

for dir in supabase/functions/*/; do
  name=$(basename "$dir")
  # A leading underscore is the Supabase convention for shared code, not a
  # deployable function.
  case "$name" in _*) continue ;; esac
  echo "Deploying $name to $1 ($PROJECT_REF)"
  supabase functions deploy "$name" --project-ref "$PROJECT_REF"
done
