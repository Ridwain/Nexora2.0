# Section 10: Tenant Contact Live Search, Filters, And Sorting

## Summary
Section 10 makes the Contacts tab useful as tenant CRM contact data grows.

Implemented behavior:

```text
Contacts tab
-> type in Search contacts
-> app waits 350ms
-> backend search_crm_contacts RPC returns tenant-scoped active matches
```

Search is backend-powered, tenant-scoped, and active-contact-only. Archived contacts remain isolated in the Archived Contacts screen.

## Database And RPC
Added migration:

```text
supabase/migrations/20260515_000012_crm_contact_search.sql
```

Migration adds:

```text
pg_trgm extension
active contact search trigram index
active lifecycle filter index
active lead status filter index
active name sort index
public.search_crm_contacts(...)
private.search_crm_contacts_impl(...)
```

Public RPC:

```text
search_crm_contacts(
  p_tenant_id uuid,
  p_query text default null,
  p_lifecycle_stage text default null,
  p_lead_status text default null,
  p_sort text default 'newest',
  p_limit int default 100
)
```

RPC rules:

```text
requires authenticated user
requires private.is_tenant_employee_or_owner(p_tenant_id)
excludes archived contacts
searches first name, last name, email, phone, company name, and job title
supports lifecycle stage and lead status filters
supports newest, oldest, and name_asc sorting
caps result limit at 100
returns private.crm_contact_json
```

## Android Changes
RPC layer added:

```text
SearchCrmContactsRequest
RpcRepository.searchCrmContacts(...)
SupabaseRpcApi rpc/search_crm_contacts
SupabaseRpcRepository.searchCrmContacts(...)
```

Contacts state added:

```text
searchQuery
selectedLifecycleStage
selectedLeadStatus
selectedSort
searchContacts
isSearchLoading
searchErrorMessage
visibleContacts
```

ViewModel behavior:

```text
typing updates state immediately
query is capped at 120 characters
backend search waits 350ms after typing
new searches cancel older searches
stale results cannot overwrite newer search results
blank query with no filters returns to cached active contacts
search results do not overwrite full active contacts cache
```

Contacts tab UI added:

```text
search input
lifecycle stage filter menu
lead status filter menu
sort menu
subtle Searching state
search empty state
search error state
clear search action
```

Dashboard behavior remains unchanged:

```text
Dashboard contact count uses full active contacts.
Dashboard recent contacts use full active contacts.
Contacts search/filter results do not corrupt Dashboard data.
```

## Verification
Android tests added:

```text
ContactsSearchViewModelTest
```

Coverage includes:

```text
query max length
debounce before backend call
debounced backend search
stale search result protection
filter-triggered search
clear search back to cached active list
search result isolation from Dashboard/full cache
```

Manual smoke test:

```text
open company workspace
open Contacts tab
type contact name/email/company
verify results update after short delay
filter lifecycle stage
filter lead status
sort by newest, oldest, and name
clear search
verify normal active contacts return
verify archived contacts do not appear
```

## Deferred Work
Not included in Section 10:

```text
pagination UI
infinite scroll
saved views
advanced duplicate detection
full-text ranking
archived contact search
customer account linking
companies/deals/tickets persistence
```
