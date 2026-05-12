# Section 4: Android Supabase Client Foundation

Project: `NexoraAndroid2`

Supabase project: `Nexora2.0`

Project ref: `hxnfjcfwbuqctgoxmgzv`

## Purpose

This section adds the Android client foundation for Supabase Auth and Section 3 RPC calls. It does not add the real login/signup screens yet.

## Network Architecture

The app uses Retrofit, OkHttp, Gson, Hilt, and DataStore.

Base URLs:

- Auth: `https://hxnfjcfwbuqctgoxmgzv.supabase.co/auth/v1/`
- REST/RPC: `https://hxnfjcfwbuqctgoxmgzv.supabase.co/rest/v1/`

Every request includes:

- `apikey: BuildConfig.SUPABASE_PUBLISHABLE_KEY`
- `Accept: application/json`

Authenticated RPC and logout requests also include:

- `Authorization: Bearer <access_token>`

## Auth Foundation

Prepared email/password operations:

- `signUp(email, password)`
- `signIn(email, password)`
- `refreshSession(refreshToken)`
- `signOut()`
- `currentSession`

Sessions are stored in DataStore through `SessionStore`. Logout clears both the auth session and any pending invite token.

## RPC Foundation

Prepared Section 3 RPC calls:

- `ensure_user_profile`
- `get_user_contexts`
- `create_owner_tenant`
- `ensure_customer_identity`
- `create_employee_invitation`
- `accept_employee_invitation`
- `create_customer_invitation`
- `accept_customer_invitation`

The RPC repository reads the current stored session before every authenticated call. If no session exists, it returns a stable `NexoraError.Unauthorized`.

## Invite Deep Links

Supported invite links:

- `nexora://employee-invite/<token>`
- `nexora://customer-invite/<token>`

`MainActivity` captures invite links on launch and on new intents. Valid invite tokens are stored in DataStore until accepted or logout clears them.

## Splash Behavior

On app start:

1. Read stored session.
2. If a session exists, attempt refresh.
3. If refresh succeeds, call `get_user_contexts`.
4. Navigate to welcome for now.

Real authenticated navigation and context selection are deferred to Section 5.

## Deferred

- Real login/signup UI.
- Role onboarding UI.
- Auth confirmation or magic-link callbacks.
- Google/social login.
- Email delivery for invites.
- Realtime subscriptions.
- Production encryption/key management.
