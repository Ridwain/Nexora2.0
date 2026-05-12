# Section 3: Backend RPC Layer

Project: `Nexora2.0`

Project ref: `hxnfjcfwbuqctgoxmgzv`

This section adds the RPC layer that Android will use for identity setup, context loading, owner enrollment, customer identity setup, and invite creation/acceptance.

## Migration Order

1. `20260505_000005_rpc_shared_helpers.sql`
2. `20260505_000006_rpc_identity_and_contexts.sql`
3. `20260505_000007_rpc_enrollment_and_invitations.sql`

## Public RPCs

- `ensure_user_profile`
- `get_user_contexts`
- `create_owner_tenant`
- `ensure_customer_identity`
- `create_employee_invitation`
- `accept_employee_invitation`
- `create_customer_invitation`
- `accept_customer_invitation`

Public RPCs are authenticated-only wrappers. Business logic runs in private schema implementations so security-definer functions are not exposed directly as public REST RPCs.

## Invite Behavior

- Raw invite tokens are returned once to Android.
- Only token hashes are stored in database tables.
- Invite links use:
  - `nexora://employee-invite/<token>`
  - `nexora://customer-invite/<token>`
- Invite acceptance requires the logged-in user's JWT email to match the invite email.
- Logged-out invite handling is deferred to Android. Android stores the token and calls the accept RPC after login/signup.

## Context Shape

Context RPCs return one normalized shape for owner, employee, and customer contexts:

```json
{
  "context_id": "owner:<tenant_id>",
  "role": "owner",
  "tenant_id": "...",
  "tenant_name": "...",
  "membership_id": "...",
  "employee_profile_id": null,
  "customer_identity_id": null,
  "customer_tenant_profile_id": null,
  "status": "active",
  "created_at": "..."
}
```

## Deferred

- Email delivery through Edge Functions.
- Production encryption/key management for `encrypted_profile_blob`.
- Android networking/session implementation.
- CRM modules such as tickets, contacts, calls, chatbot, and Jira.

