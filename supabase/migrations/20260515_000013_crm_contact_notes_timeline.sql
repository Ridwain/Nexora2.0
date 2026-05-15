create table public.crm_contact_notes (
  id uuid primary key default extensions.gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  contact_id uuid not null references public.crm_contacts(id) on delete cascade,
  body text not null,
  created_by uuid references auth.users(id) on delete set null,
  updated_by uuid references auth.users(id) on delete set null,
  archived_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint crm_contact_notes_body_not_blank check (length(trim(body)) > 0),
  constraint crm_contact_notes_body_max_length check (char_length(body) <= 2000)
);

create index crm_contact_notes_tenant_contact_created_at_idx
on public.crm_contact_notes(tenant_id, contact_id, created_at desc);

create index crm_contact_notes_tenant_archived_at_idx
on public.crm_contact_notes(tenant_id, archived_at);

create index crm_contact_notes_created_by_idx
on public.crm_contact_notes(created_by);

create index crm_contact_notes_updated_by_idx
on public.crm_contact_notes(updated_by);

create trigger crm_contact_notes_set_updated_at
before update on public.crm_contact_notes
for each row execute function public.set_updated_at();

alter table public.crm_contact_notes enable row level security;

create policy "Tenant staff can read active contact notes"
on public.crm_contact_notes
for select
to authenticated
using (
  archived_at is null
  and private.is_tenant_employee_or_owner(tenant_id)
);

create policy "Service role manages contact notes"
on public.crm_contact_notes
for all
to service_role
using (true)
with check (true);

create or replace function private.crm_contact_note_json(p_note public.crm_contact_notes)
returns jsonb
language sql
stable
security definer
set search_path = public, private, pg_temp
as $$
  select jsonb_build_object(
    'id', p_note.id,
    'type', 'note',
    'body', p_note.body,
    'event_type', null,
    'actor_user_id', p_note.created_by,
    'created_at', p_note.created_at
  );
$$;

create or replace function private.require_active_crm_contact(
  p_tenant_id uuid,
  p_contact_id uuid
)
returns void
language plpgsql
stable
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_contact_id uuid;
begin
  if p_tenant_id is null then
    raise exception 'tenant_id_required' using errcode = '22023';
  end if;

  if p_contact_id is null then
    raise exception 'contact_id_required' using errcode = '22023';
  end if;

  select id
  into v_contact_id
  from public.crm_contacts
  where id = p_contact_id
    and tenant_id = p_tenant_id
    and archived_at is null;

  if v_contact_id is null then
    raise exception 'contact_not_found' using errcode = '02000';
  end if;
end;
$$;

create or replace function private.list_contact_timeline_impl(
  p_tenant_id uuid,
  p_contact_id uuid
)
returns setof jsonb
language plpgsql
stable
security definer
set search_path = public, private, pg_temp
as $$
begin
  perform private.require_authenticated();

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  perform private.require_active_crm_contact(p_tenant_id, p_contact_id);

  return query
  with timeline_items as (
    select private.crm_contact_note_json(n) as item,
           n.created_at
    from public.crm_contact_notes n
    where n.tenant_id = p_tenant_id
      and n.contact_id = p_contact_id
      and n.archived_at is null

    union all

    select jsonb_build_object(
             'id', a.id,
             'type', 'event',
             'body', null,
             'event_type', a.event_type,
             'actor_user_id', a.actor_user_id,
             'created_at', a.created_at
           ) as item,
           a.created_at
    from public.audit_events a
    where a.tenant_id = p_tenant_id
      and a.event_type in (
        'crm_contact_created',
        'crm_contact_updated',
        'crm_contact_archived',
        'crm_contact_restored',
        'crm_contact_note_created'
      )
      and (a.event_payload ->> 'contact_id')::uuid = p_contact_id
  )
  select item
  from timeline_items
  order by created_at desc;
end;
$$;

create or replace function public.list_contact_timeline(
  p_tenant_id uuid,
  p_contact_id uuid
)
returns setof jsonb
language sql
stable
set search_path = public, private, pg_temp
as $$
  select * from private.list_contact_timeline_impl(p_tenant_id, p_contact_id);
$$;

create or replace function private.create_contact_note_impl(
  p_tenant_id uuid,
  p_contact_id uuid,
  p_body text
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_body text;
  v_note public.crm_contact_notes;
begin
  v_user_id := private.require_authenticated();

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  perform private.require_active_crm_contact(p_tenant_id, p_contact_id);

  v_body := trim(coalesce(p_body, ''));

  if v_body = '' then
    raise exception 'note_body_required' using errcode = '22023';
  end if;

  if char_length(v_body) > 2000 then
    raise exception 'note_body_too_long' using errcode = '22023';
  end if;

  insert into public.crm_contact_notes (
    tenant_id,
    contact_id,
    body,
    created_by,
    updated_by
  )
  values (
    p_tenant_id,
    p_contact_id,
    v_body,
    v_user_id,
    v_user_id
  )
  returning * into v_note;

  perform private.audit_event(
    'crm_contact_note_created',
    p_tenant_id,
    jsonb_build_object('contact_id', p_contact_id, 'note_id', v_note.id)
  );

  return private.crm_contact_note_json(v_note);
end;
$$;

create or replace function public.create_contact_note(
  p_tenant_id uuid,
  p_contact_id uuid,
  p_body text
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.create_contact_note_impl(p_tenant_id, p_contact_id, p_body);
$$;

revoke all on function public.list_contact_timeline(uuid, uuid) from public;
revoke all on function public.list_contact_timeline(uuid, uuid) from anon;
grant execute on function public.list_contact_timeline(uuid, uuid) to authenticated;

revoke all on function public.create_contact_note(uuid, uuid, text) from public;
revoke all on function public.create_contact_note(uuid, uuid, text) from anon;
grant execute on function public.create_contact_note(uuid, uuid, text) to authenticated;

grant execute on function private.crm_contact_note_json(public.crm_contact_notes) to authenticated;
grant execute on function private.require_active_crm_contact(uuid, uuid) to authenticated;
grant execute on function private.list_contact_timeline_impl(uuid, uuid) to authenticated;
grant execute on function private.create_contact_note_impl(uuid, uuid, text) to authenticated;
