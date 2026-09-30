# Auth templates

The emails Supabase Auth sends itself, separately from the Resend
templates. Sign-in is code-only, so there is one: `sign-in-code.html`.

Set by hand in the dashboard, **Authentication → Emails → Templates**,
in two slots. `signInWithOtp` picks one or the other depending on whether
the user is new, so both carry the same email:

| Slot | Subject | Body |
|---|---|---|
| Magic link | `Your Muster sign-in code: {{ .Token }}` | `sign-in-code.html` |
| Confirm signup | `Your Muster sign-in code: {{ .Token }}` | `sign-in-code.html` |

Edit here first, then paste into every environment. Variables are
Supabase's Go template ones, not Resend's: `{{ .Token }}` is the code.

The other Auth emails (change email, invite, reset password,
reauthentication) are never sent by the app and stay on their defaults.
