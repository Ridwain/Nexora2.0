create or replace function private.get_crm_contact_impl(
  p_tenant_id uuid,
  p_contact_id uuid
)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_contact public.crm_contacts;
begin
  perform private.require_authenticated();

  if p_tenant_id is null then
    raise exception 'tenant_id_required' using errcode = '22023';
  end if;

  if p_contact_id is null then
    raise exception 'contact_id_required' using errcode = '22023';
  end if;

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  select *
  into v_contact
  from public.crm_contacts
  where id = p_contact_id
    and tenant_id = p_tenant_id
    and archived_at is null;

  if v_contact.id is null then
    raise exception 'contact_not_found' using errcode = '02000';
  end if;

  return private.crm_contact_json(v_contact);
end;
$$;

create or replace function public.get_crm_contact(
  p_tenant_id uuid,
  p_contact_id uuid
)
returns jsonb
language sql
stable
set search_path = public, private, pg_temp
as $$
  select private.get_crm_contact_impl(p_tenant_id, p_contact_id);
$$;

create or replace function private.update_crm_contact_impl(
  p_tenant_id uuid,
  p_contact_id uuid,
  p_first_name text,
  p_last_name text default null,
  p_email text default null,
  p_phone text default null,
  p_company_name text default null,
  p_job_title text default null,
  p_lifecycle_stage text default 'lead',
  p_lead_status text default 'new',
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
  v_lifecycle_stage text;
  v_lead_status text;
begin
  v_user_id := private.require_authenticated();

  if p_tenant_id is null then
    raise exception 'tenant_id_required' using errcode = '22023';
  end if;

  if p_contact_id is null then
    raise exception 'contact_id_required' using errcode = '22023';
  end if;

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  if nullif(trim(p_first_name), '') is null then
    raise exception 'first_name_required' using errcode = '22023';
  end if;

  v_lifecycle_stage := coalesce(nullif(trim(p_lifecycle_stage), ''), 'lead');
  v_lead_status := coalesce(nullif(trim(p_lead_status), ''), 'new');

  if v_lifecycle_stage not in ('lead', 'subscriber', 'customer', 'evangelist', 'other') then
    raise exception 'invalid_lifecycle_stage' using errcode = '22023';
  end if;

  if v_lead_status not in ('new', 'open', 'in_progress', 'qualified', 'unqualified') then
    raise exception 'invalid_lead_status' using errcode = '22023';
  end if;

  update public.crm_contacts
  set first_name = trim(p_first_name),
      last_name = nullif(trim(p_last_name), ''),
      email = nullif(lower(trim(p_email)), ''),
      phone = nullif(trim(p_phone), ''),
      company_name = nullif(trim(p_company_name), ''),
      job_title = nullif(trim(p_job_title), ''),
      lifecycle_stage = v_lifecycle_stage,
      lead_status = v_lead_status,
      source = nullif(trim(p_source), ''),
      notes = nullif(trim(p_notes), ''),
      updated_by = v_user_id
  where id = p_contact_id
    and tenant_id = p_tenant_id
    and archived_at is null
  returning * into v_contact;

  if v_contact.id is null then
    raise exception 'contact_not_found' using errcode = '02000';
  end if;

  perform private.audit_event(
    'crm_contact_updated',
    p_tenant_id,
    jsonb_build_object('contact_id', v_contact.id, 'email', v_contact.email)
  );

  return private.crm_contact_json(v_contact);
end;
$$;

create or replace function public.update_crm_contact(
  p_tenant_id uuid,
  p_contact_id uuid,
  p_first_name text,
  p_last_name text default null,
  p_email text default null,
  p_phone text default null,
  p_company_name text default null,
  p_job_title text default null,
  p_lifecycle_stage text default 'lead',
  p_lead_status text default 'new',
  p_source text default null,
  p_notes text default null
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.update_crm_contact_impl(
    p_tenant_id,
    p_contact_id,
    p_first_name,
    p_last_name,
    p_email,
    p_phone,
    p_company_name,
    p_job_title,
    p_lifecycle_stage,
    p_lead_status,
    p_source,
    p_notes
  );
$$;

create or replace function private.archive_crm_contact_impl(
  p_tenant_id uuid,
  p_contact_id uuid
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

  if p_contact_id is null then
    raise exception 'contact_id_required' using errcode = '22023';
  end if;

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  update public.crm_contacts
  set archived_at = now(),
      updated_by = v_user_id
  where id = p_contact_id
    and tenant_id = p_tenant_id
    and archived_at is null
  returning * into v_contact;

  if v_contact.id is null then
    raise exception 'contact_not_found' using errcode = '02000';
  end if;

  perform private.audit_event(
    'crm_contact_archived',
    p_tenant_id,
    jsonb_build_object('contact_id', v_contact.id, 'email', v_contact.email)
  );

  return private.crm_contact_json(v_contact);
end;
$$;

create or replace function public.archive_crm_contact(
  p_tenant_id uuid,
  p_contact_id uuid
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.archive_crm_contact_impl(p_tenant_id, p_contact_id);
$$;

revoke all on function public.get_crm_contact(uuid, uuid) from public;
revoke all on function public.get_crm_contact(uuid, uuid) from anon;
grant execute on function public.get_crm_contact(uuid, uuid) to authenticated;

revoke all on function public.update_crm_contact(uuid, uuid, text, text, text, text, text, text, text, text, text, text) from public;
revoke all on function public.update_crm_contact(uuid, uuid, text, text, text, text, text, text, text, text, text, text) from anon;
grant execute on function public.update_crm_contact(uuid, uuid, text, text, text, text, text, text, text, text, text, text) to authenticated;

revoke all on function public.archive_crm_contact(uuid, uuid) from public;
revoke all on function public.archive_crm_contact(uuid, uuid) from anon;
grant execute on function public.archive_crm_contact(uuid, uuid) to authenticated;

grant execute on function private.get_crm_contact_impl(uuid, uuid) to authenticated;
grant execute on function private.update_crm_contact_impl(uuid, uuid, text, text, text, text, text, text, text, text, text, text) to authenticated;
grant execute on function private.archive_crm_contact_impl(uuid, uuid) to authenticated;
