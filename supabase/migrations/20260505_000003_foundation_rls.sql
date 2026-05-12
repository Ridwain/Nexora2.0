alter table public.user_profile enable row level security;
alter table public.tenants enable row level security;
alter table public.user_memberships enable row level security;
alter table public.employee_profiles enable row level security;
alter table public.customer_identity enable row level security;
alter table public.customer_tenant_profiles enable row level security;
alter table public.employee_invitations enable row level security;
alter table public.customer_invitations enable row level security;
alter table public.tenant_key_registry enable row level security;
alter table public.audit_events enable row level security;

create policy "Users can read own profile"
on public.user_profile
for select
to authenticated
using (auth.uid() = user_id);

create policy "Users can insert own profile"
on public.user_profile
for insert
to authenticated
with check (auth.uid() = user_id);

create policy "Users can update own profile"
on public.user_profile
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "Service role manages profiles"
on public.user_profile
for all
to service_role
using (true)
with check (true);

create policy "Members can read their tenants"
on public.tenants
for select
to authenticated
using (public.is_tenant_member(id));

create policy "Owners can update their tenants"
on public.tenants
for update
to authenticated
using (public.is_tenant_owner(id))
with check (public.is_tenant_owner(id));

create policy "Service role manages tenants"
on public.tenants
for all
to service_role
using (true)
with check (true);

create policy "Users can read own memberships"
on public.user_memberships
for select
to authenticated
using (auth.uid() = user_id);

create policy "Owners can read tenant memberships"
on public.user_memberships
for select
to authenticated
using (public.is_tenant_owner(tenant_id));

create policy "Service role manages memberships"
on public.user_memberships
for all
to service_role
using (true)
with check (true);

create policy "Employees can read own employee profiles"
on public.employee_profiles
for select
to authenticated
using (auth.uid() = user_id);

create policy "Owners can read tenant employee profiles"
on public.employee_profiles
for select
to authenticated
using (public.is_tenant_owner(tenant_id));

create policy "Owners can update tenant employee profiles"
on public.employee_profiles
for update
to authenticated
using (public.is_tenant_owner(tenant_id))
with check (public.is_tenant_owner(tenant_id));

create policy "Service role manages employee profiles"
on public.employee_profiles
for all
to service_role
using (true)
with check (true);

create policy "Customers can read own customer identity"
on public.customer_identity
for select
to authenticated
using (auth.uid() = user_id);

create policy "Customers can insert own customer identity"
on public.customer_identity
for insert
to authenticated
with check (auth.uid() = user_id);

create policy "Customers can update own customer identity"
on public.customer_identity
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "Service role manages customer identity"
on public.customer_identity
for all
to service_role
using (true)
with check (true);

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
using (public.is_tenant_employee_or_owner(tenant_id));

create policy "Service role manages customer tenant profiles"
on public.customer_tenant_profiles
for all
to service_role
using (true)
with check (true);

create policy "Owners can read employee invitations"
on public.employee_invitations
for select
to authenticated
using (public.is_tenant_owner(tenant_id));

create policy "Owners can update employee invitations"
on public.employee_invitations
for update
to authenticated
using (public.is_tenant_owner(tenant_id))
with check (public.is_tenant_owner(tenant_id));

create policy "Owners can delete employee invitations"
on public.employee_invitations
for delete
to authenticated
using (public.is_tenant_owner(tenant_id));

create policy "Service role manages employee invitations"
on public.employee_invitations
for all
to service_role
using (true)
with check (true);

create policy "Owners can read customer invitations"
on public.customer_invitations
for select
to authenticated
using (public.is_tenant_owner(tenant_id));

create policy "Owners can update customer invitations"
on public.customer_invitations
for update
to authenticated
using (public.is_tenant_owner(tenant_id))
with check (public.is_tenant_owner(tenant_id));

create policy "Owners can delete customer invitations"
on public.customer_invitations
for delete
to authenticated
using (public.is_tenant_owner(tenant_id));

create policy "Service role manages customer invitations"
on public.customer_invitations
for all
to service_role
using (true)
with check (true);

create policy "Owners can read tenant key metadata"
on public.tenant_key_registry
for select
to authenticated
using (public.is_tenant_owner(tenant_id));

create policy "Service role manages tenant key metadata"
on public.tenant_key_registry
for all
to service_role
using (true)
with check (true);

create policy "Users can read own audit events"
on public.audit_events
for select
to authenticated
using (actor_user_id = auth.uid());

create policy "Owners can read tenant audit events"
on public.audit_events
for select
to authenticated
using (tenant_id is not null and public.is_tenant_owner(tenant_id));

create policy "Service role manages audit events"
on public.audit_events
for all
to service_role
using (true)
with check (true);

