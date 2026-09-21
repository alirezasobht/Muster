import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY")!;
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WEBHOOK_SECRET = Deno.env.get("GROUP_INVITATION_WEBHOOK_SECRET")!;

const supabase = createClient(
  SUPABASE_URL,
  SUPABASE_SERVICE_ROLE_KEY
);

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
          from: "Muster <noreply@send.musterapp.fyi>",
          to: invitation.email,
          template: {
            id: "group-member-invitation",
            variables: {
              GROUP_NAME: groupName,
              INVITER_NAME: inviterName,
              RECIPIENT_EMAIL: invitation.email,
              CONTACT_EMAIL: "muster.team.app@gmail.com",
              ANDROID_APP_URL: "YOUR_PLAY_STORE_URL",
              WEB_APP_URL: "https://musterteamapp.netlify.app/",
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