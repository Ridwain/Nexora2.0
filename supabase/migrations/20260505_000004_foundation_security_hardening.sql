create schema if not exists private;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create or replace function private.is_tenant_member(p_tenant_id uuid)
returns boolean
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if auth.uid() is null then
    return false;
  end if;

  return exists (
    select 1
    from public.user_memberships um
    where um.tenant_id = p_tenant_id
      and um.user_id = auth.uid()
      and um.status = 'active'
  );
end;
$$;

create or replace function private.is_tenant_owner(p_tenant_id uuid)
returns boolean
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if auth.uid() is null then
    return false;
  end if;

  return exists (
    select 1
    from public.user_memberships um
    where um.tenant_id = p_tenant_id
      and um.user_id = auth.uid()
      and um.role = 'owner'
      and um.status = 'active'
  );
end;
$$;

create or replace function private.is_tenant_employee_or_owner(p_tenant_id uuid)
returns boolean
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if auth.uid() is null then
    return false;
  end if;

  return exists (
    select 1
    from public.user_memberships um
    where um.tenant_id = p_tenant_id
      and um.user_id = auth.uid()
      and um.role in ('owner', 'employee')
      and um.status = 'active'
  );
end;
$$;

grant usage on schema private to authenticated;
grant execute on function private.is_tenant_member(uuid) to authenticated;
grant execute on function private.is_tenant_owner(uuid) to authenticated;
grant execute on function private.is_tenant_employee_or_owner(uuid) to authenticated;

drop policy if exists "Members can read their tenants" on public.tenants;
drop policy if exists "Owners can update their tenants" on public.tenants;
drop policy if exists "Users can read own memberships" on public.user_memberships;
drop policy if exists "Owners can read tenant memberships" on public.user_memberships;
drop policy if exists "Employees can read own employee profiles" on public.employee_profiles;
drop policy if exists "Owners can read tenant employee profiles" on public.employee_profiles;
drop policy if exists "Owners can update tenant employee profiles" on public.employee_profiles;
drop policy if exists "Customers can read own tenant profiles" on public.customer_tenant_profiles;
drop policy if exists "Tenant staff can read customer tenant profiles" on public.customer_tenant_profiles;
drop policy if exists "Owners can read employee invitations" on public.employee_invitations;
drop policy if exists "Owners can update employee invitations" on public.employee_invitations;
drop policy if exists "Owners can delete employee invitations" on public.employee_invitations;
drop policy if exists "Owners can read customer invitations" on public.customer_invitations;
drop policy if exists "Owners can update customer invitations" on public.customer_invitations;
drop policy if exists "Owners can delete customer invitations" on public.customer_invitations;
drop policy if exists "Owners can read tenant key metadata" on public.tenant_key_registry;
drop policy if exists "Owners can read tenant audit events" on public.audit_events;

create policy "Members can read their tenants"
on public.tenants
for select
to authenticated
using (private.is_tenant_member(id));

create policy "Owners can update their tenants"
on public.tenants
for update
to authenticated
using (private.is_tenant_owner(id))
with check (private.is_tenant_owner(id));

create policy "Users can read own memberships"
on public.user_memberships
for select
to authenticated
using (auth.uid() = user_id);

create policy "Owners can read tenant memberships"
on public.user_memberships
for select
to authenticated
using (private.is_tenant_owner(tenant_id));

create policy "Employees can read own employee profiles"
on public.employee_profiles
for select
to authenticated
using (auth.uid() = user_id);

create policy "Owners can read tenant employee profiles"
on public.employee_profiles
for select
to authenticated
using (private.is_tenant_owner(tenant_id));

create policy "Owners can update tenant employee profiles"
on public.employee_profiles
for update
to authenticated
using (private.is_tenant_owner(tenant_id))
with check (private.is_tenant_owner(tenant_id));

create policy "Customers can read own tenant profiles"
on public.customer_tenant_profiles
for select
to authenticated
using (
  exists (
    select 1
    from public.customer_identity ci
    where ci.id = customer_tenant_profiles.customer_identity_id
      and ci.user_id = auth.uid()
  )
);

create policy "Tenant staff can read customer tenant profiles"
on public.customer_tenant_profiles
for select
to authenticated
using (private.is_tenant_employee_or_owner(tenant_id));

create policy "Owners can read employee invitations"
on public.employee_invitations
for select
to authenticated
using (private.is_tenant_owner(tenant_id));

create policy "Owners can update employee invitations"
on public.employee_invitations
for update
to authenticated
using (private.is_tenant_owner(tenant_id))
with check (private.is_tenant_owner(tenant_id));

create policy "Owners can delete employee invitations"
on public.employee_invitations
for delete
to authenticated
using (private.is_tenant_owner(tenant_id));

create policy "Owners can read customer invitations"
on public.customer_invitations
for select
to authenticated
using (private.is_tenant_owner(tenant_id));

create policy "Owners can update customer invitations"
on public.customer_invitations
for update
to authenticated
using (private.is_tenant_owner(tenant_id))
with check (private.is_tenant_owner(tenant_id));

create policy "Owners can delete customer invitations"
on public.customer_invitations
for delete
to authenticated
using (private.is_tenant_owner(tenant_id));

create policy "Owners can read tenant key metadata"
on public.tenant_key_registry
for select
to authenticated
using (private.is_tenant_owner(tenant_id));

create policy "Owners can read tenant audit events"
on public.audit_events
for select
to authenticated
using (tenant_id is not null and private.is_tenant_owner(tenant_id));

drop function if exists public.is_tenant_member(uuid);
drop function if exists public.is_tenant_owner(uuid);
drop function if exists public.is_tenant_employee_or_owner(uuid);

