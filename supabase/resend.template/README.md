# Resend templates

The HTML for each Resend template. The subject and template ID live in
Resend, not here, so the subject is recorded below. Keep both in step
with the dashboard.

| File | Subject | Template ID secret |
|---|---|---|
| `group-member-invitation.html` | You've been invited to join {{GROUP_NAME}} | `RESEND_MEMBER_INVITATION_TEMPLATE_ID` |
| `event-invitation.html` | Invitation for {{GROUP_NAME}}: {{EVENT_TITLE}}, {{EVENT_TIME}} | `RESEND_EVENT_INVITATION_TEMPLATE_ID` |

## Variables

**group-member-invitation:** `GROUP_NAME`, `INVITER_NAME`,
`RECIPIENT_EMAIL`, `CONTACT_EMAIL`, `ANDROID_APP_URL`, `WEB_APP_URL`

**event-invitation:** `GROUP_NAME`, `EVENT_TITLE`, `EVENT_TIME`,
`EVENT_LOCATION`, `RECIPIENT_EMAIL`, `CONTACT_EMAIL`, `ANDROID_APP_URL`,
`WEB_APP_URL`
