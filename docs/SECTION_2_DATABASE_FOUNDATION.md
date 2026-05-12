# Section 2: Supabase Database Foundation

Project: `Nexora2.0`

Project ref: `hxnfjcfwbuqctgoxmgzv`

This section creates the database foundation for the one-identity, multi-context architecture.

## Migration Order

1. `20260505_000001_foundation_extensions_and_helpers.sql`
2. `20260505_000002_foundation_tables.sql`
3. `20260505_000003_foundation_rls.sql`
4. `20260505_000004_foundation_security_hardening.sql`

## Tables

- `user_profile`: global identity profile for each Supabase Auth user.
- `tenants`: business workspaces.
- `user_memberships`: canonical owner, employee, and customer role contexts.
- `employee_profiles`: tenant-scoped employee profiles.
- `customer_identity`: one global customer identity per auth user.
- `customer_tenant_profiles`: tenant-scoped customer relationship records with encrypted tenant-private data.
- `employee_invitations`: employee invite token records.
- `customer_invitations`: customer invite token records.
- `tenant_key_registry`: tenant encryption key metadata.
- `audit_events`: append-only audit event storage.

## RLS Summary

All foundation tables have RLS enabled.

- Users can read and update their own identity/customer rows.
- Users can read their own memberships.
- Tenant owners can read/update tenant-controlled records.
- Invitation tables do not allow public or anon reads.
- Direct client writes for enrollment are intentionally blocked until Section 3 RPCs exist.
- Tenant membership helper functions live in the private schema so they are not exposed as public REST RPCs.

## Deferred To Section 3

- `get_user_contexts`
- owner tenant creation RPC
- customer identity creation/upsert RPC
- employee/customer invitation creation RPCs
- employee/customer invitation validation and acceptance RPCs
- audit writes from business operations
- encrypted customer profile write/read RPCs

## Verification

Use Supabase MCP against project `hxnfjcfwbuqctgoxmgzv`:

```text
list_tables(project_id="hxnfjcfwbuqctgoxmgzv", schemas=["public"], verbose=false)
list_migrations(project_id="hxnfjcfwbuqctgoxmgzv")
get_advisors(project_id="hxnfjcfwbuqctgoxmgzv", type="security")
```

Expected:

- 10 public tables exist.
- RLS is enabled for every foundation table.
- No public/anon invitation table access exists.
- No Edge Functions are required for this section.
