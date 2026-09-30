-- Muster: send_group_invitation_email is internal until a resend action
-- exists.
--
-- It was granted to authenticated so a future "resend invitation" button
-- could call it directly. Until that ships, the grant only means any admin
-- can call it in a loop and send the invitee unlimited email through
-- Resend, with no throttle, last_sent_at or send_count to stop it.
--
-- invite_group_member_by_email still calls it: that function is security
-- definer and runs as its owner, which keeps EXECUTE.
--
-- Re-grant when resend is built, and add the throttle in the same
-- migration rather than after.
revoke execute on function public.send_group_invitation_email(uuid)
from authenticated;
