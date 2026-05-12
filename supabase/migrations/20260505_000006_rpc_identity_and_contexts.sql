create or replace function private.ensure_user_profile_impl(
  p_display_name text default null,
  p_phone text default null,
  p_timezone text default null
)
returns public.user_profile
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_email text;
  v_profile public.user_profile;
begin
  v_user_id := private.require_authenticated();
  v_email := private.current_user_email();

  insert into public.user_profile (
    user_id,
    email,
    display_name,
    phone,
    timezone,
    is_active
  )
  values (
    v_user_id,
    v_email,
    nullif(p_display_name, ''),
    nullif(p_phone, ''),
    nullif(p_timezone, ''),
    true
  )
  on conflict (user_id) do update
  set
    email = excluded.email,
    display_name = coalesce(nullif(p_display_name, ''), public.user_profile.display_name),
    phone = coalesce(nullif(p_phone, ''), public.user_profile.phone),
    timezone = coalesce(nullif(p_timezone, ''), public.user_profile.timezone),
    is_active = true
  returning * into v_profile;

  perform private.audit_event('user_profile_ensured', null, jsonb_build_object('email', v_email));

  return v_profile;
end;
$$;

create or replace function public.ensure_user_profile(
  p_display_name text default null,
  p_phone text default null,
  p_timezone text default null
)
returns public.user_profile
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select * from private.ensure_user_profile_impl(p_display_name, p_phone, p_timezone);
$$;

create or replace function private.get_user_contexts_impl()
returns table (
  context_id text,
  role text,
  tenant_id uuid,
  tenant_name text,
  membership_id uuid,
  employee_profile_id uuid,
  customer_identity_id uuid,
  customer_tenant_profile_id uuid,
  status text,
  created_at timestamptz
)
language sql
stable
security definer
set search_path = public, private, pg_temp
as $$
  select
    'owner:' || t.id::text as context_id,
    'owner'::text as role,
    t.id as tenant_id,
    t.name as tenant_name,
    um.id as membership_id,
    null::uuid as employee_profile_id,
    null::uuid as customer_identity_id,
    null::uuid as customer_tenant_profile_id,
    um.status,
    um.created_at
  from public.user_memberships um
  join public.tenants t on t.id = um.tenant_id
  where um.user_id = private.require_authenticated()
    and um.role = 'owner'
    and um.status = 'active'
    and t.status = 'active'

  union all

  select
    'employee:' || ep.id::text as context_id,
    'employee'::text as role,
    t.id as tenant_id,
    t.name as tenant_name,
    um.id as membership_id,
    ep.id as employee_profile_id,
    null::uuid as customer_identity_id,
    null::uuid as customer_tenant_profile_id,
    um.status,
    um.created_at
  from public.user_memberships um
  join public.tenants t on t.id = um.tenant_id
  join public.employee_profiles ep
    on ep.user_id = um.user_id
   and ep.tenant_id = um.tenant_id
  where um.user_id = private.require_authenticated()
    and um.role = 'employee'
    and um.status = 'active'
    and ep.status = 'active'
    and t.status = 'active'

  union all

  select
    'customer:' || ctp.id::text as context_id,
    'customer'::text as role,
    t.id as tenant_id,
    t.name as tenant_name,
    um.id as membership_id,
    null::uuid as employee_profile_id,
    ci.id as customer_identity_id,
    ctp.id as customer_tenant_profile_id,
    um.status,
    um.created_at
  from public.user_memberships um
  join public.tenants t on t.id = um.tenant_id
  join public.customer_identity ci on ci.user_id = um.user_id
  join public.customer_tenant_profiles ctp
    on ctp.customer_identity_id = ci.id
   and ctp.tenant_id = um.tenant_id
  where um.user_id = private.require_authenticated()
    and um.role = 'customer'
    and um.status = 'active'
    and ci.status = 'active'
    and ctp.relationship_status = 'active'
    and t.status = 'active'
  order by created_at asc;
$$;

create or replace function public.get_user_contexts()
returns table (
  context_id text,
  role text,
  tenant_id uuid,
  tenant_name text,
  membership_id uuid,
  employee_profile_id uuid,
  customer_identity_id uuid,
  customer_tenant_profile_id uuid,
  status text,
  created_at timestamptz
)
language sql
stable
set search_path = public, private, pg_temp
as $$
  select * from private.get_user_contexts_impl();
$$;

revoke all on function public.ensure_user_profile(text, text, text) from public;
revoke all on function public.ensure_user_profile(text, text, text) from anon;
grant execute on function public.ensure_user_profile(text, text, text) to authenticated;

revoke all on function public.get_user_contexts() from public;
revoke all on function public.get_user_contexts() from anon;
grant execute on function public.get_user_contexts() to authenticated;

grant execute on function private.ensure_user_profile_impl(text, text, text) to authenticated;
grant execute on function private.get_user_contexts_impl() to authenticated;

