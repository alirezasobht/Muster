-- Tracks when an event invitation's email was last sent, so a resend action
-- can be throttled. Null until the first send.

alter table public.event_invitations
    add column last_sent_at timestamptz;
