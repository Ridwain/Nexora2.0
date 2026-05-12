# Section 9: Contact Detail, Edit, And Archive

## Summary
Section 9 makes tenant-scoped CRM contacts manageable.

After Section 8, users could add and list contacts. Section 9 adds:

```text
Tap contact -> Contact detail
Edit contact -> Save updated fields
Archive contact -> Hide from active lists
```

Archive is a soft delete. It sets `archived_at` and keeps the row in `public.crm_contacts`.

## Database And RPC
Added migration:

```text
supabase/migrations/20260512_000010_crm_contact_detail_edit_archive.sql
```

Public RPCs added:

```text
public.get_crm_contact(p_tenant_id uuid, p_contact_id uuid)
public.update_crm_contact(...)
public.archive_crm_contact(p_tenant_id uuid, p_contact_id uuid)
```

Private implementation functions enforce:

```text
private.require_authenticated()
private.is_tenant_employee_or_owner(p_tenant_id)
```

Behavior:

```text
get_crm_contact returns one non-archived tenant contact.
update_crm_contact trims fields, requires first_name, validates lifecycle/status, updates updated_by, and writes crm_contact_updated audit event.
archive_crm_contact sets archived_at, updates updated_by, writes crm_contact_archived audit event, and removes the contact from active list results.
```

No direct authenticated update/delete is exposed from Android. Writes stay behind RPCs.

## Android Changes
RPC layer added:

```text
RpcRepository.getCrmContact(tenantId, contactId)
RpcRepository.updateCrmContact(...)
RpcRepository.archiveCrmContact(tenantId, contactId)
SupabaseRpcApi rpc/get_crm_contact
SupabaseRpcApi rpc/update_crm_contact
SupabaseRpcApi rpc/archive_crm_contact
```

Contacts memory cache was extended:

```text
contactFor(tenantId, contactId)
upsert(tenantId, contact)
remove(tenantId, contactId)
```

New screens:

```text
ContactDetailScreen
EditContactScreen
```

New routes:

```text
contact_detail/{tenantId}/{tenantName}/{contactId}
edit_contact/{tenantId}/{tenantName}/{contactId}
```

Workspace route now supports an initial tab query so archive can return the user to Contacts:

```text
workspace/{contextId}/{role}/{tenantId}/{tenantName}?initialTab=Contacts
```

## User-Facing Behavior
Contacts list and Dashboard recent contacts are clickable.

Contact detail shows:

```text
full name
email
phone
company name
job title
lifecycle stage
lead status
source
notes
created date
updated date
```

Edit contact:

```text
prefills current contact values
uses same max-length rules as Add Contact
requires first name
validates email if provided
keeps form values on failed save
updates cache after successful save
returns to detail
```

Archive contact:

```text
shows confirm dialog
Cancel keeps contact unchanged
Archive hides contact from active lists
archive success removes contact from memory cache
archive success returns to workspace Contacts tab
```

## Tenant Isolation
Section 9 keeps the same isolation model as Section 8:

```text
Contact row belongs to one tenant_id.
RPCs require owner/employee access for that tenant.
Archived contacts do not appear in list_crm_contacts.
Other tenants cannot get, edit, or archive the contact.
```

## Verification Completed
Android verification:

```text
./gradlew :app:testDebugUnitTest
```

passed after adding detail/edit/archive ViewModel tests.

Supabase verification to run after migration:

```text
list_migrations
execute function existence checks
get_advisors security
get_advisors performance
```

Result:

```text
crm_contact_detail_edit_archive migration is applied.
get_crm_contact, update_crm_contact, and archive_crm_contact exist in public schema.
private implementation functions exist.
Security advisor has no Section 9-specific issue.
Performance advisor shows older tuning/unused-index items, not a new Section 9 blocker.
```

Final Android build verification:

```text
./gradlew :app:assembleDebug
```

passed.

Manual smoke test:

```text
create a contact
tap contact card and open detail
edit contact and verify detail/list/dashboard update
archive contact and verify it disappears from active list
reopen company and verify archived contact stays hidden
```

## Deferred Work
Not included in Section 9:

```text
hard delete
restore archived contact
contact activity timeline
notes history
search
pagination
duplicate detection
customer account linking
companies/deals/tickets persistence
```
