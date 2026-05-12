create table public.crm_contacts (
  id uuid primary key default extensions.gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  customer_tenant_profile_id uuid references public.customer_tenant_profiles(id) on delete set null,
  first_name text not null,
  last_name text,
  email extensions.citext,
  phone text,
  company_name text,
  job_title text,
  lifecycle_stage text not null default 'lead',
  lead_status text not null default 'new',
  source text,
  notes text,
  created_by uuid references auth.users(id) on delete set null,
  updated_by uuid references auth.users(id) on delete set null,
  archived_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint crm_contacts_lifecycle_stage_check check (
    lifecycle_stage in ('lead', 'subscriber', 'customer', 'evangelist', 'other')
  ),
  constraint crm_contacts_lead_status_check check (
    lead_status in ('new', 'open', 'in_progress', 'qualified', 'unqualified')
  )
);

create index crm_contacts_tenant_id_idx on public.crm_contacts(tenant_id);
create index crm_contacts_tenant_created_at_idx on public.crm_contacts(tenant_id, created_at desc);
create index crm_contacts_tenant_email_idx on public.crm_contacts(tenant_id, email);
create index crm_contacts_tenant_archived_at_idx on public.crm_contacts(tenant_id, archived_at);
create index crm_contacts_customer_tenant_profile_id_idx on public.crm_contacts(customer_tenant_profile_id);

create trigger crm_contacts_set_updated_at
before update on public.crm_contacts
for each row execute function public.set_updated_at();

alter table public.crm_contacts enable row level security;

create policy "Tenant staff can read active crm contacts"
on public.crm_contacts
for select
to authenticated
using (
  archived_at is null
  and private.is_tenant_employee_or_owner(tenant_id)
);

create policy "Service role manages crm contacts"
on public.crm_contacts
for all
to service_role
using (true)
with check (true);

create or replace function private.crm_contact_json(p_contact public.crm_contacts)
returns jsonb
language sql
stable
security definer
set search_path = public, private, pg_temp
as $$
  select jsonb_build_object(
    'id', p_contact.id,
    'tenant_id', p_contact.tenant_id,
    'customer_tenant_profile_id', p_contact.customer_tenant_profile_id,
    'first_name', p_contact.first_name,
    'last_name', p_contact.last_name,
    'email', p_contact.email,
    'phone', p_contact.phone,
    'company_name', p_contact.company_name,
    'job_title', p_contact.job_title,
    'lifecycle_stage', p_contact.lifecycle_stage,
    'lead_status', p_contact.lead_status,
    'source', p_contact.source,
    'notes', p_contact.notes,
    'created_at', p_contact.created_at,
    'updated_at', p_contact.updated_at
  );
$$;

create or replace function private.list_crm_contacts_impl(p_tenant_id uuid)
returns setof jsonb
language plpgsql
stable
security definer
set search_path = public, private, pg_temp
as $$
begin
  perform private.require_authenticated();

  if p_tenant_id is null then
    raise exception 'tenant_id_required' using errcode = '22023';
  end if;

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  return query
  select private.crm_contact_json(c)
  from public.crm_contacts c
  where c.tenant_id = p_tenant_id
    and c.archived_at is null
  order by c.created_at desc;
end;
$$;

create or replace function public.list_crm_contacts(p_tenant_id uuid)
returns setof jsonb
language sql
stable
set search_path = public, private, pg_temp
as $$
  select * from private.list_crm_contacts_impl(p_tenant_id);
$$;

create or replace function private.create_crm_contact_impl(
  p_tenant_id uuid,
  p_first_name text,
  p_last_name text default null,
  p_email text default null,
  p_phone text default null,
  p_company_name text default null,
  p_job_title text default null,
  p_source text default null,
  p_notes text default null
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_contact public.crm_contacts;
begin
  v_user_id := private.require_authenticated();

  if p_tenant_id is null then
    raise exception 'tenant_id_required' using errcode = '22023';
  end if;

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  if nullif(trim(p_first_name), '') is null then
    raise exception 'first_name_required' using errcode = '22023';
  end if;

  insert into public.crm_contacts (
    tenant_id,
    first_name,
    last_name,
    email,
    phone,
    company_name,
    job_title,
    source,
    notes,
    created_by,
    updated_by
  )
  values (
    p_tenant_id,
    trim(p_first_name),
    nullif(trim(p_last_name), ''),
    nullif(lower(trim(p_email)), ''),
    nullif(trim(p_phone), ''),
    nullif(trim(p_company_name), ''),
    nullif(trim(p_job_title), ''),
    nullif(trim(p_source), ''),
    nullif(trim(p_notes), ''),
    v_user_id,
    v_user_id
  )
  returning * into v_contact;

  perform private.audit_event(
    'crm_contact_created',
    p_tenant_id,
    jsonb_build_object('contact_id', v_contact.id, 'email', v_contact.email)
  );

  return private.crm_contact_json(v_contact);
end;
$$;

create or replace function public.create_crm_contact(
  p_tenant_id uuid,
  p_first_name text,
  p_last_name text default null,
  p_email text default null,
  p_phone text default null,
  p_company_name text default null,
  p_job_title text default null,
  p_source text default null,
  p_notes text default null
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.create_crm_contact_impl(
    p_tenant_id,
    p_first_name,
    p_last_name,
    p_email,
    p_phone,
    p_company_name,
    p_job_title,
    p_source,
    p_notes
  );
$$;

revoke all on function public.list_crm_contacts(uuid) from public;
revoke all on function public.list_crm_contacts(uuid) from anon;
grant execute on function public.list_crm_contacts(uuid) to authenticated;

revoke all on function public.create_crm_contact(uuid, text, text, text, text, text, text, text, text) from public;
revoke all on function public.create_crm_contact(uuid, text, text, text, text, text, text, text, text) from anon;
grant execute on function public.create_crm_contact(uuid, text, text, text, text, text, text, text, text) to authenticated;

grant execute on function private.crm_contact_json(public.crm_contacts) to authenticated;
grant execute on function private.list_crm_contacts_impl(uuid) to authenticated;
grant execute on function private.create_crm_contact_impl(uuid, text, text, text, text, text, text, text, text) to authenticated;
