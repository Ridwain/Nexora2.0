# Section 11: Contact Notes And Timeline Foundation

## Summary
Section 11 adds a tenant-scoped timeline to Contact Detail.

Implemented behavior:

```text
Contact Detail
-> Timeline
-> Add note
-> note appears at top of timeline
```

Notes are private CRM records for the selected tenant workspace. They are not customer-visible.

## Database And RPC
Added migration:

```text
supabase/migrations/20260515_000013_crm_contact_notes_timeline.sql
```

Added table:

```text
public.crm_contact_notes
```

Important fields:

```text
tenant_id
contact_id
body
created_by
updated_by
archived_at
created_at
updated_at
```

Added RPCs:

```text
public.list_contact_timeline(p_tenant_id uuid, p_contact_id uuid)
public.create_contact_note(p_tenant_id uuid, p_contact_id uuid, p_body text)
```

RPC rules:

```text
requires authenticated user
requires private.is_tenant_employee_or_owner(p_tenant_id)
requires active contact in the same tenant
rejects blank notes
rejects notes over 2000 characters
excludes archived notes
writes crm_contact_note_created audit event
```

Timeline combines:

```text
crm_contact_notes rows
selected audit_events for the same contact_id
```

Timeline item JSON:

```text
id
type: note | event
body
event_type
actor_user_id
created_at
```

## Android Changes
Domain/data layer added:

```text
ContactTimelineItem
ContactTimelineItemType
ContactTimelineItemDto
ListContactTimelineRequest
CreateContactNoteRequest
RpcRepository.listContactTimeline(...)
RpcRepository.createContactNote(...)
SupabaseRpcApi rpc/list_contact_timeline
SupabaseRpcApi rpc/create_contact_note
```

Contact Detail state added:

```text
timelineItems
isTimelineLoading
timelineErrorMessage
noteBody
isAddingNote
addNoteErrorMessage
```

UI added:

```text
Timeline card
multiline Add a note input
Add note button
timeline loading state
timeline empty state
timeline retry state
note rows
activity event rows
```

Input safety:

```text
note body capped at 2000 characters
blank note blocked before network call
network call only happens when Add note is tapped
failed create keeps input visible
successful create clears input and prepends note
```

## Verification
Android tests added to `ContactDetailViewModelTest`:

```text
timeline load success
timeline load failure keeps contact detail visible
note body max length
blank note blocks RPC
valid note creates and prepends timeline item
note create failure keeps input and shows error
```

Manual smoke test:

```text
open company workspace
open Contacts
open a contact detail
add a note
verify note appears at top of timeline
close/reopen contact
verify note still appears
verify another tenant cannot see the note
```

## Deferred Work
Not included in Section 11:

```text
note edit
note delete/archive UI
note pinning
mentions
attachments
tasks/reminders
customer-visible notes
realtime timeline updates
full activity timeline design
```
