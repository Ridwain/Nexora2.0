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
    'archived_at', p_contact.archived_at,
    'created_at', p_contact.created_at,
    'updated_at', p_contact.updated_at
  );
$$;

create or replace function private.list_archived_crm_contacts_impl(p_tenant_id uuid)
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
    and c.archived_at is not null
  order by c.archived_at desc, c.created_at desc;
end;
$$;

create or replace function public.list_archived_crm_contacts(p_tenant_id uuid)
returns setof jsonb
language sql
stable
set search_path = public, private, pg_temp
as $$
  select * from private.list_archived_crm_contacts_impl(p_tenant_id);
$$;

create or replace function private.restore_crm_contact_impl(
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
  set archived_at = null,
      updated_by = v_user_id
  where id = p_contact_id
    and tenant_id = p_tenant_id
    and archived_at is not null
  returning * into v_contact;

  if v_contact.id is null then
    raise exception 'contact_not_found' using errcode = '02000';
  end if;

  perform private.audit_event(
    'crm_contact_restored',
    p_tenant_id,
    jsonb_build_object('contact_id', v_contact.id, 'email', v_contact.email)
  );

  return private.crm_contact_json(v_contact);
end;
$$;

create or replace function public.restore_crm_contact(
  p_tenant_id uuid,
  p_contact_id uuid
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.restore_crm_contact_impl(p_tenant_id, p_contact_id);
$$;

revoke all on function public.list_archived_crm_contacts(uuid) from public;
revoke all on function public.list_archived_crm_contacts(uuid) from anon;
grant execute on function public.list_archived_crm_contacts(uuid) to authenticated;

revoke all on function public.restore_crm_contact(uuid, uuid) from public;
revoke all on function public.restore_crm_contact(uuid, uuid) from anon;
grant execute on function public.restore_crm_contact(uuid, uuid) to authenticated;

grant execute on function private.list_archived_crm_contacts_impl(uuid) to authenticated;
grant execute on function private.restore_crm_contact_impl(uuid, uuid) to authenticated;
