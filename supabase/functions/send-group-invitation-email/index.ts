import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

// Injected by Supabase into every deployed function; never set by hand.
const SUPABASE_URL = requireEnv("SUPABASE_URL");
const SUPABASE_SERVICE_ROLE_KEY = requireEnv("SUPABASE_SERVICE_ROLE_KEY");

// Edge Function secrets, per environment. See env-examples/edge.env.example.
const WEBHOOK_SECRET = requireEnv("GROUP_INVITATION_WEBHOOK_SECRET");
const RESEND_API_KEY = requireEnv("RESEND_API_KEY");
const RESEND_FROM = requireEnv("RESEND_FROM");
const RESEND_MEMBER_INVITATION_TEMPLATE_ID = requireEnv("RESEND_MEMBER_INVITATION_TEMPLATE_ID");
const CONTACT_EMAIL = requireEnv("CONTACT_EMAIL");
const WEB_APP_URL = requireEnv("WEB_APP_URL");

// Read at module load, so a missing secret fails the deploy's first request
// with a clear log line instead of sending an email with an empty field.
function requireEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`missing environment variable: ${name}`);
  return value;
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

export default {
  async fetch(req: Request) {
    if (req.headers.get("x-webhook-secret") !== WEBHOOK_SECRET) {
      return new Response("unauthorized", { status: 401 });
    }
    try {
      const { invitation_id } = await req.json();

      if (!invitation_id) {
        return new Response("missing invitation_id", { status: 400 });
      }

      const { data: invitation, error } = await supabase
        .from("group_invitations")
        .select(`
          email,
          status,
          groups(name),
          profiles!group_invitations_invited_by_fkey(name)
        `)
        .eq("id", invitation_id)
        .single();

      if (error || !invitation) {
        return new Response("invitation not found", { status: 404 });
      }

      if (invitation.status !== "pending") {
        return new Response("invitation is not pending", { status: 400 });
      }

      const groupName = invitation.groups?.name ?? "your group";
      const inviterName = invitation.profiles?.name ?? "A group admin";

      const resendResponse = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${RESEND_API_KEY}`,
        },
        body: JSON.stringify({
          from: RESEND_FROM,
          to: invitation.email,
          template: {
            id: RESEND_MEMBER_INVITATION_TEMPLATE_ID,
            variables: {
              GROUP_NAME: groupName,
              INVITER_NAME: inviterName,
              RECIPIENT_EMAIL: invitation.email,
              CONTACT_EMAIL: CONTACT_EMAIL,
              WEB_APP_URL: WEB_APP_URL,
            },
          },
        }),
      });

      if (!resendResponse.ok) {
        console.error(await resendResponse.text());
        return new Response("failed to send email", { status: 500 });
      }

      return new Response("ok");
    } catch (e) {
      console.error(e);
      return new Response("internal error", { status: 500 });
    }
  },
};
