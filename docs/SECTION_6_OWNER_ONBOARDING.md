# Section 6: Owner-First Role Onboarding

Project: `NexoraAndroid2`

Supabase project: `Nexora2.0`

Project ref: `hxnfjcfwbuqctgoxmgzv`

## Purpose

Section 6 turns the authenticated empty context state into the first usable workspace flow. A verified logged-in user can create a company and become its owner using the existing `create_owner_tenant` RPC.

This section is owner-first only. Customer onboarding, employee onboarding, invite creation, invite acceptance, CRM dashboards, encryption changes, and database migrations are deferred.

## User Flow

1. User logs in and reaches `ContextPicker`.
2. If the user has no contexts, the empty state now includes `Create company`.
3. `Create company` opens owner onboarding.
4. User enters company details.
5. App calls `create_owner_tenant`.
6. On success, app returns to `ContextPicker`.
7. `ContextPicker` reloads `get_user_contexts` and shows the new owner context.
8. User taps the owner context card to open the tenant CRM shell added in Section 7.

Users can create multiple companies. `Create company` remains available even after owner contexts exist.

## Android Changes

- Added owner onboarding route: `owner_onboarding`.
- Added owner workspace placeholder route. Section 7 replaces it with reusable `workspace/{contextId}/{role}/{tenantName}`.
- Added `CreateOwnerTenantScreen` and `CreateOwnerTenantViewModel`.
- Added owner form input limits and validation.
- Updated `ContextPickerScreen`:
  - always exposes `Create company`
  - keeps `Refresh`
  - keeps pending invite notice
  - makes owner context cards clickable
  - shows `Open` for owner contexts and `Soon` for non-owner contexts
- Added `OwnerWorkspaceScreen` as a placeholder for future owner dashboard work. Section 7 replaces this placeholder with `TenantWorkspaceScreen`.

## RPC Behavior

Section 6 uses existing Section 3 backend RPCs only:

```text
create_owner_tenant
get_user_contexts
```

No new database migration is required.

Owner create request values:

```text
name: required company name
industry: optional
country: optional
timezone: ZoneId.systemDefault().id
email: optional company email
phone: optional company phone
```

On success, `create_owner_tenant` returns a normalized owner `UserContext`. The app then returns to `ContextPicker`, which refreshes contexts from the backend.

## Validation

Input is stored in ViewModel state and capped before it reaches UI state:

- Company name: `120`, required
- Industry: `80`, optional
- Country: `80`, optional
- Company email: `254`, optional, must contain `@` if present
- Company phone: `32`, optional

Network calls happen only when the user taps `Create company`. Submit is disabled while loading.

## Error Behavior

- Missing company name blocks the RPC call.
- Invalid optional company email blocks the RPC call.
- RPC failure keeps the user on the owner onboarding form.
- Form values remain after failure.
- Successful create navigates away, preventing duplicate submit.

## Owner Workspace

Section 6 originally created a minimal owner workspace placeholder. Section 7 replaces it with a tenant-aware CRM shell.

The original placeholder showed:

- tenant/company name
- role label: `Owner workspace`
- note that CRM dashboard, employees, customers, and settings start later

Real persisted owner dashboard modules are still deferred to later sections.

## Test Coverage

Unit tests cover:

- company name validation before network call
- optional email validation
- long input length caps
- no network calls while typing
- successful `createOwnerTenant` request values
- failure preserving form values and showing error

Verification commands:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Manual Smoke Test

1. Login with a verified account.
2. Confirm `ContextPicker` shows `Create company`.
3. Create a company.
4. Confirm the app returns to `ContextPicker`.
5. Confirm owner context appears.
6. Tap owner context.
7. Confirm owner workspace placeholder opens.
8. Reopen the app and confirm owner context persists.
9. Create a second company and confirm multiple owner contexts show.

Database checks:

```text
public.tenants has a new row
public.user_memberships has an owner membership
get_user_contexts returns owner context
```

## Deferred

- Owner dashboard modules.
- Tenant settings.
- Employee invite creation UI.
- Customer invite creation UI.
- Customer self-profile onboarding.
- Employee/customer invite acceptance UI.
- Tenant encryption and key-management UX.
