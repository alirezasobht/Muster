import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

// Injected by Supabase into every deployed function; never set by hand.
const SUPABASE_URL = requireEnv("SUPABASE_URL");
const SUPABASE_SERVICE_ROLE_KEY = requireEnv("SUPABASE_SERVICE_ROLE_KEY");

// Edge Function secrets, per environment.
const WEBHOOK_SECRET = requireEnv("EVENT_INVITATION_WEBHOOK_SECRET");
const RESEND_API_KEY = requireEnv("RESEND_API_KEY");
const RESEND_FROM = requireEnv("RESEND_FROM");
const RESEND_EVENT_INVITATION_TEMPLATE_ID = requireEnv("RESEND_EVENT_INVITATION_TEMPLATE_ID");
const CONTACT_EMAIL = requireEnv("CONTACT_EMAIL");
const WEB_APP_URL = requireEnv("WEB_APP_URL");

// Resend's batch endpoint accepts at most 100 emails per request.
const BATCH_LIMIT = 100;

// Every group plays in Sydney for now; see CONTEXT.md → Time zones.
const eventTimeFormat = new Intl.DateTimeFormat("en-AU", {
  timeZone: "Australia/Sydney",
  weekday: "short",
  day: "numeric",
  month: "short",
  hour: "numeric",
  minute: "2-digit",
});

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
      const { invitation_ids } = await req.json();

      if (!Array.isArray(invitation_ids) || invitation_ids.length === 0) {
        return new Response("missing invitation_ids", { status: 400 });
      }

      // A status can change before this runs, so only pending invitations
      // are sent; the rest are skipped, not failed.
      const { data: invitations, error } = await supabase
        .from("event_invitations")
        .select(`
          profile_id,
          events(title, starts_at, location, groups(name))
        `)
        .in("id", [...new Set(invitation_ids)])
        .eq("status", "pending");

      if (error) {
        console.error(error);
        return new Response("failed to read invitations", { status: 500 });
      }

      if (!invitations?.length) {
        return new Response("nothing to send");
      }

      // event_invitations reaches profiles only through group_members, so
      // there is no direct embed; emails are read separately.
      const { data: profiles, error: profilesError } = await supabase
        .from("profiles")
        .select("id, email")
        .in("id", invitations.map((i) => i.profile_id));

      if (profilesError) {
        console.error(profilesError);
        return new Response("failed to read profiles", { status: 500 });
      }

      const emailById = new Map(profiles.map((p) => [p.id, p.email]));

      const emails = invitations.flatMap((invitation) => {
        const email = emailById.get(invitation.profile_id);
        const event = invitation.events;
        if (!email || !event) return [];

        return [{
          from: RESEND_FROM,
          to: email,
          template: {
            id: RESEND_EVENT_INVITATION_TEMPLATE_ID,
            variables: {
              GROUP_NAME: event.groups?.name ?? "your group",
              EVENT_TITLE: event.title,
              EVENT_TIME: eventTimeFormat.format(new Date(event.starts_at)),
              EVENT_LOCATION: event.location ?? "Location to be confirmed",
              RECIPIENT_EMAIL: email,
              CONTACT_EMAIL: CONTACT_EMAIL,
              WEB_APP_URL: WEB_APP_URL,
            },
          },
        }];
      });

      for (let start = 0; start < emails.length; start += BATCH_LIMIT) {
        const resendResponse = await fetch("https://api.resend.com/emails/batch", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${RESEND_API_KEY}`,
          },
          body: JSON.stringify(emails.slice(start, start + BATCH_LIMIT)),
        });

        if (!resendResponse.ok) {
          console.error(await resendResponse.text());
          return new Response("failed to send emails", { status: 500 });
        }
      }

      return new Response("ok");
    } catch (e) {
      console.error(e);
      return new Response("internal error", { status: 500 });
    }
  },
};
