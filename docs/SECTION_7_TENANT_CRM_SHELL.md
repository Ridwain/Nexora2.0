# Section 7: Tenant CRM Shell + UI Foundation

Project: `NexoraAndroid2`

Supabase project: `Nexora2.0`

Project ref: `hxnfjcfwbuqctgoxmgzv`

## Purpose

Section 7 replaces the simple owner workspace placeholder with a tenant-aware CRM shell. This makes the app feel like a SaaS CRM workspace after an owner opens a company context.

This section is UI/client architecture only. It does not add CRM database tables, migrations, or new Supabase RPCs.

## User Flow

```text
Login
-> Context Picker
-> Tap owner company
-> CRM Workspace Shell
```

The CRM shell shows the selected tenant/company name and role. Back from the CRM shell returns to Context Picker. Back from Context Picker still moves the app to the background.

## Android Changes

- Replaced `OwnerWorkspaceScreen` with `TenantWorkspaceScreen`.
- Added reusable workspace route:
  ```text
  workspace/{contextId}/{role}/{tenantName}
  ```
- Added workspace models:
  ```text
  SelectedWorkspaceContext
  CrmWorkspaceTab
  ```
- Owner context cards now open the reusable CRM shell.
- Non-owner cards remain future-state/disabled through the existing `Soon` behavior.

## CRM Shell Tabs

The workspace shell has mobile-first bottom navigation:

- Dashboard
- Contacts
- Companies
- Deals
- Tickets
- Settings

All tabs are placeholders in Section 7. They do not read or write Supabase CRM data yet.

## Dashboard UI

The Dashboard tab includes:

- tenant workspace header
- placeholder metric cards
- quick actions
- CRM module preview card

Current quick actions are UI-only:

- Add contact
- New deal
- Create ticket

These actions intentionally do not persist data yet.

## Tenant Isolation Copy

Every CRM module placeholder explains tenant scoping. Future CRM data created inside a workspace must belong only to the selected tenant/company.

Examples:

```text
Contacts added here will belong only to this company workspace.
Deals and pipelines here will be scoped to this tenant only.
Support tickets here will be visible only to this company workspace.
```

## Deferred

- Contacts database tables and RPCs.
- Companies database tables and RPCs.
- Deals and pipelines.
- Tickets and support inbox.
- Tasks, notes, calls, and messages.
- Workspace settings persistence.
- Employee/customer workspace permission differences.
- Persisting selected workspace locally.

## Verification

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Manual smoke test:

1. Login with a verified owner account.
2. Open Context Picker.
3. Tap an owner company card.
4. Confirm CRM shell opens with the correct tenant name.
5. Switch Dashboard, Contacts, Companies, Deals, Tickets, and Settings tabs.
6. Confirm each placeholder mentions tenant scoping.
7. Press Back and confirm Context Picker appears.
8. Press Back from Context Picker and confirm app moves to background.
