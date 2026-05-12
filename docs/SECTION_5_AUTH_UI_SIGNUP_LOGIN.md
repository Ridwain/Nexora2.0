# Section 5: Auth UI + Signup/Login Flow

Project: `NexoraAndroid2`

Supabase project: `Nexora2.0`

Project ref: `hxnfjcfwbuqctgoxmgzv`

## Purpose

This section turns the scaffold into a real email/password auth app. Signup is intentionally one-path only: after creating the account, the user verifies an 8 digit email OTP inside the Android app and then logs in. The app does not enter directly from signup or OTP verification.

Section 5 was later extended with OTP recovery behavior so an unfinished signup can be continued after the app is closed and reopened.

## User Flow

1. App opens to splash, then welcome.
2. User chooses `Login` or `Create account`.
3. Signup collects full name, email, password, and confirm password.
4. Signup calls Supabase Auth and navigates to the OTP verification screen on success.
5. Supabase sends an email containing an 8 digit code.
6. User enters the code in Nexora.
7. OTP verification confirms the account, clears any returned session, and sends the user to Login.
8. Login calls `ensure_user_profile`, then `get_user_contexts`.
9. User lands on the context picker.

## What Changed In Android

Section 5 added the first complete auth UI layer:

- Welcome buttons are enabled and route to Login or Create account.
- Login, Signup, OTP verification, auth message, and context picker screens are wired through Navigation Compose.
- `LoginViewModel`, `SignupViewModel`, `VerifyEmailOtpViewModel`, and `ContextPickerViewModel` own screen state and network actions.
- Signup creates only the Supabase Auth account and never enters the app directly.
- Login is the only authenticated entry path into Nexora.
- Login success creates/updates `public.user_profile` through `ensure_user_profile`.
- Context loading happens through `get_user_contexts` after profile ensuring succeeds.
- Logout clears local session and returns to Welcome.

Section 5.1 replaced clickable email confirmation links with Android-entered OTP:

- Signup success navigates to `Verify email`.
- Supabase confirmation email contains `{{ .Token }}` instead of a localhost/project redirect link.
- Android calls `POST /auth/v1/verify` with `type = signup`, `email`, and `token`.
- Android calls `POST /auth/v1/resend` to request a new signup OTP.
- OTP verification success clears any returned Supabase session and sends the user to Login.
- Display name entered during signup is kept locally until first successful login.

Section 5.2 added pending OTP recovery:

- Signup success stores pending verification data locally in DataStore: email, display name, and created timestamp.
- If the user closes the app on the OTP screen, Welcome later shows a pending verification card.
- The pending card supports `Continue verification`, `Resend code`, and `Use another email`.
- If login fails because the email is not verified, Login shows a `Verify email` action and does not call profile/context RPCs.
- OTP success marks verification complete and hides the pending card, but keeps the signup display name until first login.
- Full name is applied to `public.user_profile.display_name` after the user verifies OTP and then logs in on the same device.

Section 5.3 cleaned up auth navigation and authenticated back behavior:

- Auth screen switching no longer leaves old forms behind in the back stack.
- `Welcome -> Signup -> Login -> Back` returns to Welcome, not the old Signup form.
- `Welcome -> Login -> Signup -> Back` returns to Welcome, not the old Login form.
- Signup success, OTP recovery, and OTP completion route through auth screens without keeping stale forms in history.
- Login success clears the auth stack and opens `ContextPicker`.
- On `ContextPicker`, Android back moves the app to the background with `moveTaskToBack(true)`.
- Back from `ContextPicker` never shows Welcome/Login/Signup.
- Logout is the only user action that clears session and returns to Welcome from the authenticated area.

## Email OTP Verification

Supabase should have email confirmation enabled for production, but the confirmation email template uses `{{ .Token }}` instead of `{{ .ConfirmationURL }}`.

The Confirm Signup email template is:

```html
<h2>Your Nexora verification code</h2>
<p>Enter this code in the Nexora app to confirm your account:</p>
<h1>{{ .Token }}</h1>
<p>This code expires soon. If you did not request this, ignore this email.</p>
```

Because verification happens in Android, signup confirmation no longer depends on `Site URL` redirects.

Current Supabase OTP length is `8`, so Android validates exactly 8 digits.

## Pending Verification Recovery

Supabase creates a pending row in `auth.users` as soon as signup succeeds. Until OTP verification is completed, `email_confirmed_at` remains null and password login is blocked by Supabase.

Nexora treats this pending Auth row as incomplete account setup. The app does not create `public.user_profile`, tenant data, customer data, or employee data at signup time.

Local recovery data is stored only on the device where signup started:

```text
email
displayName
createdAtEpochSeconds
```

This local state allows the app to restore the OTP flow if the user exits before entering the code. On the same device, the user's full name is preserved and later passed into `ensure_user_profile(displayName)` after successful login.

After OTP verification succeeds, Nexora stops showing the pending verification card but keeps the local display name record. The display name record is cleared only after first successful login creates/updates `public.user_profile`.

If the user switches devices, the local full name is not available on the new device. The user can still verify/login, but preserving signup metadata across devices would require a future server-side pending signup store.

## Profile Creation Timing

`public.user_profile` is created or updated only after login succeeds.

```text
Signup submit
-> Supabase auth.users pending row
-> OTP verification confirms auth user
-> Login with email/password
-> ensure_user_profile(displayName)
-> public.user_profile row
-> get_user_contexts
-> Context picker
```

This keeps unverified users out of Nexora business tables.

## Navigation And Back Behavior

Auth screens use a shallow auth stack rooted at Welcome. Moving between Login, Signup, and OTP replaces the active auth screen instead of preserving old form screens behind it.

Expected unauthenticated behavior:

```text
Welcome -> Signup -> Login -> Back = Welcome
Welcome -> Login -> Signup -> Back = Welcome
Welcome -> pending OTP card -> OTP -> Back = Login
OTP verified -> Login -> Back = Welcome
```

Expected authenticated behavior:

```text
Login success -> ContextPicker
ContextPicker -> Back = app moves to background
Reopen app -> stored session refreshes and returns to authenticated flow
ContextPicker -> Logout = Welcome
```

This prevents users from seeing auth forms after they have already logged in, while still allowing normal Android backgrounding from the authenticated root screen.

## Input Safety

Auth input is stored in ViewModel state and capped before it reaches UI state:

- Display name: `80`
- Email: `254`
- Password: `128`
- Confirm password: `128`
- OTP: `8`, digits only

Typing or pasting very long text should not crash the app. Network calls happen only when the user taps submit, never on each keystroke.

## Error And Recovery Behavior

- Wrong OTP keeps the user on the OTP screen and shows the Supabase error.
- Expired OTP can be replaced with `Resend code`.
- Signup with an already-started email shows a recovery message and exposes `Verify email`.
- Login with an unverified email shows `Verify email` and blocks app entry.
- Signup, OTP verification, and unverified login all clear/avoid local authenticated entry state.
- Accidental sessions returned by Supabase signup or OTP verification are cleared so Login remains the only entry path.
- OTP verification does not delete the saved signup display name; login success deletes it after `ensure_user_profile` succeeds.

## Context Picker

The context picker shows owner, employee, and customer contexts returned by `get_user_contexts`.

If no contexts exist yet, it shows a clean empty state. If a pending invite token exists, it shows a notice that invite acceptance starts in Section 6.

## Deferred

- Role onboarding.
- Owner tenant creation UI.
- Customer profile onboarding.
- Employee/customer invite acceptance.
- Auth deep-link callback handling.
- Google/social login.
- Cross-device pending signup metadata preservation.
- Business dashboards.
- Database migrations.
