create extension if not exists pg_trgm with schema extensions;

create index if not exists crm_contacts_active_search_trgm_idx
on public.crm_contacts
using gin (
  (lower(
    coalesce(first_name, '') || ' ' ||
    coalesce(last_name, '') || ' ' ||
    coalesce(email::text, '') || ' ' ||
    coalesce(phone, '') || ' ' ||
    coalesce(company_name, '') || ' ' ||
    coalesce(job_title, '')
  )) extensions.gin_trgm_ops
)
where archived_at is null;

create index if not exists crm_contacts_active_lifecycle_idx
on public.crm_contacts(tenant_id, lifecycle_stage, created_at desc)
where archived_at is null;

create index if not exists crm_contacts_active_lead_status_idx
on public.crm_contacts(tenant_id, lead_status, created_at desc)
where archived_at is null;

create index if not exists crm_contacts_active_name_idx
on public.crm_contacts(tenant_id, lower(first_name), lower(coalesce(last_name, '')))
where archived_at is null;

create or replace function private.search_crm_contacts_impl(
  p_tenant_id uuid,
  p_query text default null,
  p_lifecycle_stage text default null,
  p_lead_status text default null,
  p_sort text default 'newest',
  p_limit int default 100
)
returns setof jsonb
language plpgsql
stable
security definer
set search_path = public, private, extensions, pg_temp
as $$
declare
  v_query text;
  v_lifecycle_stage text;
  v_lead_status text;
  v_sort text;
  v_limit int;
begin
  perform private.require_authenticated();

  if p_tenant_id is null then
    raise exception 'tenant_id_required' using errcode = '22023';
  end if;

  if not private.is_tenant_employee_or_owner(p_tenant_id) then
    raise exception 'not_tenant_staff' using errcode = '42501';
  end if;

  v_query := lower(nullif(trim(coalesce(p_query, '')), ''));
  v_lifecycle_stage := nullif(trim(coalesce(p_lifecycle_stage, '')), '');
  v_lead_status := nullif(trim(coalesce(p_lead_status, '')), '');
  v_sort := coalesce(nullif(trim(p_sort), ''), 'newest');
  v_limit := least(greatest(coalesce(p_limit, 100), 1), 100);

  if v_lifecycle_stage is not null
    and v_lifecycle_stage not in ('lead', 'subscriber', 'customer', 'evangelist', 'other') then
    raise exception 'invalid_lifecycle_stage' using errcode = '22023';
  end if;

  if v_lead_status is not null
    and v_lead_status not in ('new', 'open', 'in_progress', 'qualified', 'unqualified') then
    raise exception 'invalid_lead_status' using errcode = '22023';
  end if;

  if v_sort not in ('newest', 'oldest', 'name_asc') then
    raise exception 'invalid_sort' using errcode = '22023';
  end if;

  return query
  select private.crm_contact_json(c)
  from public.crm_contacts c
  where c.tenant_id = p_tenant_id
    and c.archived_at is null
    and (v_lifecycle_stage is null or c.lifecycle_stage = v_lifecycle_stage)
    and (v_lead_status is null or c.lead_status = v_lead_status)
    and (
      v_query is null
      or lower(
        coalesce(c.first_name, '') || ' ' ||
        coalesce(c.last_name, '') || ' ' ||
        coalesce(c.email::text, '') || ' ' ||
        coalesce(c.phone, '') || ' ' ||
        coalesce(c.company_name, '') || ' ' ||
        coalesce(c.job_title, '')
      ) like '%' || v_query || '%'
    )
  order by
    case when v_sort = 'newest' then c.created_at end desc,
    case when v_sort = 'oldest' then c.created_at end asc,
    case when v_sort = 'name_asc' then lower(c.first_name) end asc,
    case when v_sort = 'name_asc' then lower(coalesce(c.last_name, '')) end asc,
    c.created_at desc
  limit v_limit;
end;
$$;

create or replace function public.search_crm_contacts(
  p_tenant_id uuid,
  p_query text default null,
  p_lifecycle_stage text default null,
  p_lead_status text default null,
  p_sort text default 'newest',
  p_limit int default 100
)
returns setof jsonb
language sql
stable
set search_path = public, private, extensions, pg_temp
as $$
  select * from private.search_crm_contacts_impl(
    p_tenant_id,
    p_query,
    p_lifecycle_stage,
    p_lead_status,
    p_sort,
    p_limit
  );
$$;

revoke all on function public.search_crm_contacts(uuid, text, text, text, text, int) from public;
revoke all on function public.search_crm_contacts(uuid, text, text, text, text, int) from anon;
grant execute on function public.search_crm_contacts(uuid, text, text, text, text, int) to authenticated;

grant execute on function private.search_crm_contacts_impl(uuid, text, text, text, text, int) to authenticated;
