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

Section 9.1 adds archived-contact recovery:

```text
Contacts tab -> Archived -> Restore contact -> Active contacts
```

## Section 9.1 Add-On: Archived Contacts Restore
After manual testing Section 9, we found a product gap: archive was reversible in the database model because it was a soft delete, but the app had no way to restore/unarchive a contact.

Section 9.1 fixed that gap without adding hard delete or changing the active contacts architecture.

Implemented flow:

```text
Contacts tab
-> Archived
-> Archived Contacts screen
-> Restore
-> Confirm restore
-> Contact returns to active Contacts list
```

Section 9.1 commit:

```text
cd50f44 Add archived contact restore flow
```

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

Section 9.1 migration added:

```text
supabase/migrations/20260512_000011_crm_contact_restore.sql
```

Additional public RPCs:

```text
public.list_archived_crm_contacts(p_tenant_id uuid)
public.restore_crm_contact(p_tenant_id uuid, p_contact_id uuid)
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
list_archived_crm_contacts returns archived contacts for the tenant only.
restore_crm_contact clears archived_at, updates updated_by, writes crm_contact_restored audit event, and makes the contact active again.
```

Section 9.1 also updates `private.crm_contact_json` to include:

```text
archived_at
```

This lets Android show when an archived contact was archived.

No direct authenticated update/delete is exposed from Android. Writes stay behind RPCs.

## Android Changes
RPC layer added:

```text
RpcRepository.getCrmContact(tenantId, contactId)
RpcRepository.updateCrmContact(...)
RpcRepository.archiveCrmContact(tenantId, contactId)
RpcRepository.listArchivedCrmContacts(tenantId)
RpcRepository.restoreCrmContact(tenantId, contactId)
SupabaseRpcApi rpc/get_crm_contact
SupabaseRpcApi rpc/update_crm_contact
SupabaseRpcApi rpc/archive_crm_contact
SupabaseRpcApi rpc/list_archived_crm_contacts
SupabaseRpcApi rpc/restore_crm_contact
```

Contacts memory cache was extended:

```text
contactFor(tenantId, contactId)
upsert(tenantId, contact)
remove(tenantId, contactId)
archivedContactsFor(tenantId)
putArchived(tenantId, contacts)
removeArchived(tenantId, contactId)
```

Section 9.1 cache behavior:

```text
Archived list is cached separately from active list.
Restore removes the contact from archived cache.
Restore upserts the contact into active contacts cache.
Returning to Contacts can show the restored record without waiting for a full reload.
```

New screens:

```text
ContactDetailScreen
EditContactScreen
ArchivedContactsScreen
```

New ViewModel:

```text
ArchivedContactsViewModel
```

New routes:

```text
contact_detail/{tenantId}/{tenantName}/{contactId}
edit_contact/{tenantId}/{tenantName}/{contactId}
archived_contacts/{tenantId}/{tenantName}
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

Archived contacts:

```text
Contacts tab has an Archived action.
Archived screen lists soft-deleted contacts for the current tenant.
Restore asks for confirmation before changing the record.
Restore moves the contact back to active contacts.
Restore updates memory cache so Contacts can show the restored record without a full reload.
```

Archived screen states:

```text
loading archived contacts
empty archived list
load error with retry
restore confirmation dialog
restore failure message while keeping the archived contact visible
```

## Tenant Isolation
Section 9 keeps the same isolation model as Section 8:

```text
Contact row belongs to one tenant_id.
RPCs require owner/employee access for that tenant.
Archived contacts do not appear in list_crm_contacts.
Archived contacts appear only in list_archived_crm_contacts.
Restored contacts appear again in list_crm_contacts.
Other tenants cannot get, edit, or archive the contact.
Other tenants cannot list archived records or restore them.
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
crm_contact_restore migration is applied.
get_crm_contact, update_crm_contact, and archive_crm_contact exist in public schema.
list_archived_crm_contacts and restore_crm_contact exist in public schema.
private implementation functions exist.
Security advisor has no Section 9-specific issue.
Performance advisor shows older tuning/unused-index items, not a new Section 9 blocker.
```

Section 9.1 Android tests added:

```text
ArchivedContactsViewModelTest
```

Test coverage includes:

```text
archived contacts load success
archived contacts first-load failure
restore requires selected contact/confirmation state
restore success moves contact from archived cache to active cache
restore failure keeps archived contact visible
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
open Archived contacts
restore contact
verify contact appears in active Contacts again
```

## Deferred Work
Not included in Section 9:

```text
hard delete
contact activity timeline
notes history
search
pagination
duplicate detection
customer account linking
companies/deals/tickets persistence
```
