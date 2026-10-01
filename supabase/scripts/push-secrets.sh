#!/bin/sh
# Uploads one environment's Edge Function secrets to its own Supabase project.
#
#   supabase/scripts/push-secrets.sh dev
#   supabase/scripts/push-secrets.sh prod
#
# Muster-env prefixes every name with its environment (DEV_RESEND_API_KEY,
# PROD_RESEND_API_KEY). This refuses any line whose prefix doesn't match the
# environment asked for, strips the prefix, and uploads the plain names the
# Edge Function reads.
set -eu
. "$(dirname "$0")/_env.sh"

EDGE_FILE="$ENV_DIR/edge.env"
[ -f "$EDGE_FILE" ] || { echo "missing $EDGE_FILE" >&2; exit 1; }

# Names only in errors, never values.
BAD=$(grep -v '^[[:space:]]*#' "$EDGE_FILE" | grep -v '^[[:space:]]*$' | grep -v "^${PREFIX}" || true)
if [ -n "$BAD" ]; then
  echo "refusing: lines in $EDGE_FILE without the ${PREFIX} prefix:" >&2
  echo "$BAD" | cut -d= -f1 >&2
  exit 1
fi

TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT
grep "^${PREFIX}" "$EDGE_FILE" | sed "s/^${PREFIX}//" > "$TMP"

# Must match the required names in the Edge Functions. Checked before
# uploading anything, so a half-filled file never reaches Supabase. Quotes are
# stripped first: NAME="" is empty, even though it has characters after =.
for name in GROUP_INVITATION_WEBHOOK_SECRET EVENT_INVITATION_WEBHOOK_SECRET \
            RESEND_API_KEY RESEND_FROM RESEND_MEMBER_INVITATION_TEMPLATE_ID \
            RESEND_EVENT_INVITATION_TEMPLATE_ID CONTACT_EMAIL WEB_APP_URL; do
  value=$(grep "^${name}=" "$TMP" | head -n 1 | cut -d= -f2- | sed "s/^[\"']//; s/[\"']\$//")
  [ -n "$value" ] || { echo "missing or empty: ${PREFIX}${name}" >&2; exit 1; }
done

echo "Uploading $(wc -l < "$TMP" | tr -d ' ') secrets to $1 ($PROJECT_REF)"
supabase secrets set --env-file "$TMP" --project-ref "$PROJECT_REF"

# Remove anything left behind by a renamed or dropped secret — set only
# adds and updates, never removes. After the set, not before: unsetting
# first leaves the function with no secrets, failing every invite, until the
# set finishes. SUPABASE_* are injected by the platform and can't be unset.
EXISTING=$(supabase secrets list --project-ref "$PROJECT_REF" -o json \
  | grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' \
  | sed 's/.*"\([^"]*\)"$/\1/' \
  | grep -v '^SUPABASE_' || true)
WANTED=$(cut -d= -f1 "$TMP")
STALE=""
for name in $EXISTING; do
  echo "$WANTED" | grep -qx "$name" || STALE="$STALE $name"
done
if [ -n "$STALE" ]; then
  echo "Removing stale secrets:$STALE"
  # Unquoted on purpose: one argument per name.
  supabase secrets unset $STALE --project-ref "$PROJECT_REF"
fi
