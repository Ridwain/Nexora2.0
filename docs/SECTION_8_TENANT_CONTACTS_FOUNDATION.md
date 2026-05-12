# Section 8: Tenant-Scoped CRM Contacts Foundation

## Summary
Section 8 adds the first real CRM data module: tenant-scoped contacts.

Core model:

```text
Contact = tenant-owned CRM person record
Customer account = authenticated external user profile, linked later
```

A contact can exist inside one company workspace without becoming an app user. The schema keeps `customer_tenant_profile_id` nullable so a future section can link a CRM contact to a tenant-specific customer account.

## Database Changes
Added migration:

```text
supabase/migrations/20260512_000008_crm_contacts_foundation.sql
```

Added `public.crm_contacts` with tenant ownership, optional future customer-profile link, CRM person fields, lifecycle/status fields, audit creator/updater fields, archive support, and timestamps.

Important columns:

```text
tenant_id
customer_tenant_profile_id
first_name
last_name
email
phone
company_name
job_title
lifecycle_stage
lead_status
source
notes
created_by
updated_by
archived_at
created_at
updated_at
```

Validation:

```text
lifecycle_stage in lead, subscriber, customer, evangelist, other
lead_status in new, open, in_progress, qualified, unqualified
```

Indexes added for tenant-safe reads and future linking:

```text
tenant_id
tenant_id + created_at desc
tenant_id + email
tenant_id + archived_at
customer_tenant_profile_id
created_by
updated_by
```

The `created_by` and `updated_by` indexes were added in follow-up migration:

```text
supabase/migrations/20260512_000009_crm_contacts_advisor_indexes.sql
```

## RLS And RPC
RLS is enabled on `public.crm_contacts`.

Authenticated users do not directly insert/update/delete contacts from the client. Direct authenticated read is limited to active contacts for tenants where the user is an owner or employee.

Service role can manage all rows.

Public RPCs added:

```text
public.list_crm_contacts(p_tenant_id uuid)
public.create_crm_contact(...)
```

Private implementation functions enforce:

```text
private.require_authenticated()
private.is_tenant_employee_or_owner(p_tenant_id)
```

`list_crm_contacts` returns non-archived contacts for the selected tenant, newest first.

`create_crm_contact` trims input, requires first name, stores the contact under the selected tenant, sets `created_by` and `updated_by`, writes `crm_contact_created` to audit events, and returns the created contact.

## Android Architecture Changes
Workspace routing now carries the selected tenant id:

```text
workspace/{contextId}/{role}/{tenantId}/{tenantName}
```

`SelectedWorkspaceContext` now includes:

```kotlin
val tenantId: String
```

Android data layer added:

```text
CrmContactDto
CrmContact
CreateCrmContactRequest
ListCrmContactsRequest
RpcRepository.listCrmContacts(tenantId)
RpcRepository.createCrmContact(...)
SupabaseRpcApi rpc/list_crm_contacts
SupabaseRpcApi rpc/create_crm_contact
```

The existing Retrofit/OkHttp authenticated RPC stack is unchanged.

## UI Changes
The Contacts tab in the tenant CRM workspace is now real.

Contacts tab behavior:

```text
Open Contacts tab -> load tenant contacts
Loading -> show progress
Empty -> show tenant-scoped empty state + Add contact
Failure -> show readable error + Retry
Success -> show contact cards
```

Dashboard `Add contact` quick action now opens the same add-contact flow.

Added route:

```text
add_contact/{tenantId}/{tenantName}
```

Add contact fields:

```text
First name: required, max 80
Last name: optional, max 80
Email: optional, max 254, must contain @ if present
Phone: optional, max 32
Company name: optional, max 120
Job title: optional, max 120
Source: optional, max 80
Notes: optional, max 1000
```

After successful create, the app returns to the workspace and reloads contacts.

UI copy clearly states that contacts belong only to the selected company workspace.

## Post-Implementation UX Improvements
After the initial Section 8 implementation, several CRM workspace improvements were added.

Dashboard cleanup:

```text
Removed placeholder Soon cards for Deals, Tickets, and Pipeline.
Kept Dashboard focused on implemented contact functionality only.
Dashboard now shows real contacts overview count.
Dashboard now shows recent contacts preview.
Dashboard still has Add contact as a fast action.
```

Quick action navigation:

```text
Dashboard Add contact opens the contact form directly.
Back from that form returns to Dashboard.
Contacts tab Add contact still returns to Contacts tab.
```

Silent refresh behavior:

```text
First contact load can show loading.
After contacts are loaded once, returning from Add Contact keeps old count/list visible.
Background refresh runs without showing Loading or Refreshing text.
Refresh failure is non-blocking when cached contacts exist.
Refresh failure can show a retry message while keeping old contacts visible.
```

Workspace re-entry cache:

```text
Leaving company workspace back to Context Picker destroys the workspace screen.
A singleton in-memory contacts cache now keeps contacts for the app session.
Re-entering the same company uses cached contacts immediately.
Supabase refresh still runs in the background.
This avoids the Loading word flashing when opening the same company again.
```

Current cache scope:

```text
Cache is in-memory only.
Cache survives workspace route exit/re-entry during the same app process.
Cache does not survive app process death.
No Room/local database cache was added in Section 8.
```

## Tenant Isolation
Tenant isolation is enforced in three places:

```text
Database row has tenant_id
RPC checks private.is_tenant_employee_or_owner(p_tenant_id)
RLS blocks unauthorized direct reads
```

Same email can exist as separate contacts in different tenants. One company cannot list or create contacts for another company unless the current user is a valid owner/employee of that tenant.

## Verification Completed
Supabase verification:

```text
crm_contacts table exists with RLS enabled
list_crm_contacts and create_crm_contact RPCs exist
crm_contacts indexes exist
Section 8 migrations are applied
```

Android verification:

```text
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

passed after adding Section 8 ViewModel tests and post-implementation UX fixes.

Supabase advisors:

```text
Security advisor: no Section 8-specific issue found
Performance advisor: crm_contacts unindexed created_by/updated_by warnings were fixed
Remaining warnings are older schema tuning or expected unused-index reports before traffic
```

## Deferred Work
Not included in Section 8:

```text
Contact detail screen
Edit contact
Archive/delete contact
Search
Pagination
Import
Realtime
Customer account creation
Customer invite
Linking crm_contacts to customer_tenant_profiles
Companies/deals/tickets persistence
```
