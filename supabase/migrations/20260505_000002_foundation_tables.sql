create table public.user_profile (
  user_id uuid primary key references auth.users(id) on delete cascade,
  email extensions.citext not null unique,
  display_name text,
  phone text,
  avatar_url text,
  timezone text,
  language text not null default 'en',
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.tenants (
  id uuid primary key default extensions.gen_random_uuid(),
  name text not null,
  industry text,
  country text,
  timezone text,
  email extensions.citext,
  phone text,
  logo_url text,
  tier text not null default 'basic',
  status text not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint tenants_tier_check check (tier in ('basic', 'pro', 'enterprise')),
  constraint tenants_status_check check (status in ('active', 'suspended', 'inactive'))
);

create table public.user_memberships (
  id uuid primary key default extensions.gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  role text not null,
  status text not null default 'active',
  role_label text,
  assigned_by uuid references auth.users(id),
  assigned_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint user_memberships_role_check check (role in ('owner', 'employee', 'customer')),
  constraint user_memberships_status_check check (status in ('active', 'invited', 'suspended', 'inactive')),
  constraint user_memberships_unique_context unique (user_id, tenant_id, role)
);

create index user_memberships_user_id_idx on public.user_memberships(user_id);
create index user_memberships_tenant_id_idx on public.user_memberships(tenant_id);
create index user_memberships_tenant_role_idx on public.user_memberships(tenant_id, role);

create table public.employee_profiles (
  id uuid primary key default extensions.gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  membership_id uuid references public.user_memberships(id) on delete set null,
  full_name text not null,
  email extensions.citext not null,
  phone text,
  job_title text,
  department text,
  avatar_url text,
  status text not null default 'active',
  invited_by uuid references auth.users(id),
  invitation_accepted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint employee_profiles_status_check check (status in ('active', 'invited', 'suspended', 'inactive')),
  constraint employee_profiles_unique_user_tenant unique (user_id, tenant_id)
);

create index employee_profiles_tenant_id_idx on public.employee_profiles(tenant_id);
create index employee_profiles_email_idx on public.employee_profiles(email);

create table public.customer_identity (
  id uuid primary key default extensions.gen_random_uuid(),
  user_id uuid not null unique references auth.users(id) on delete cascade,
  email extensions.citext not null unique,
  first_name text not null,
  last_name text not null,
  phone text,
  avatar_url text,
  status text not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint customer_identity_status_check check (status in ('active', 'suspended', 'inactive'))
);

create table public.customer_tenant_profiles (
  id uuid primary key default extensions.gen_random_uuid(),
  customer_identity_id uuid not null references public.customer_identity(id) on delete cascade,
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  membership_id uuid references public.user_memberships(id) on delete set null,
  relationship_status text not null default 'active',
  customer_code text,
  assigned_employee_profile_id uuid references public.employee_profiles(id) on delete set null,
  encrypted_profile_blob bytea,
  encryption_key_ref text,
  encryption_key_version integer not null default 1,
  encrypted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint customer_tenant_profiles_relationship_status_check check (relationship_status in ('active', 'inactive', 'blocked')),
  constraint customer_tenant_profiles_unique_customer_tenant unique (customer_identity_id, tenant_id)
);

create index customer_tenant_profiles_tenant_id_idx on public.customer_tenant_profiles(tenant_id);
create index customer_tenant_profiles_relationship_status_idx on public.customer_tenant_profiles(relationship_status);

create table public.employee_invitations (
  id uuid primary key default extensions.gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  email extensions.citext not null,
  token_hash text not null unique,
  invited_by uuid not null references auth.users(id),
  suggested_full_name text,
  suggested_job_title text,
  status text not null default 'pending',
  expires_at timestamptz not null,
  accepted_by_user_id uuid references auth.users(id),
  accepted_at timestamptz,
  cancelled_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint employee_invitations_status_check check (status in ('pending', 'accepted', 'expired', 'cancelled'))
);

create index employee_invitations_tenant_id_idx on public.employee_invitations(tenant_id);
create index employee_invitations_email_idx on public.employee_invitations(email);
create index employee_invitations_status_idx on public.employee_invitations(status);

create table public.customer_invitations (
  id uuid primary key default extensions.gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  email extensions.citext not null,
  token_hash text not null unique,
  invited_by uuid not null references auth.users(id),
  suggested_first_name text,
  suggested_last_name text,
  status text not null default 'pending',
  expires_at timestamptz not null,
  accepted_by_user_id uuid references auth.users(id),
  accepted_customer_identity_id uuid references public.customer_identity(id),
  accepted_at timestamptz,
  cancelled_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint customer_invitations_status_check check (status in ('pending', 'accepted', 'expired', 'cancelled'))
);

create index customer_invitations_tenant_id_idx on public.customer_invitations(tenant_id);
create index customer_invitations_email_idx on public.customer_invitations(email);
create index customer_invitations_status_idx on public.customer_invitations(status);

create table public.tenant_key_registry (
  tenant_id uuid primary key references public.tenants(id) on delete cascade,
  key_ref text not null,
  key_version integer not null default 1,
  status text not null default 'active',
  created_at timestamptz not null default now(),
  rotated_at timestamptz,
  constraint tenant_key_registry_status_check check (status in ('active', 'rotating', 'retired'))
);

create table public.audit_events (
  id uuid primary key default extensions.gen_random_uuid(),
  actor_user_id uuid references auth.users(id) on delete set null,
  tenant_id uuid references public.tenants(id) on delete set null,
  event_type text not null,
  event_payload jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index audit_events_actor_user_id_idx on public.audit_events(actor_user_id);
create index audit_events_tenant_id_idx on public.audit_events(tenant_id);
create index audit_events_event_type_idx on public.audit_events(event_type);
create index audit_events_created_at_idx on public.audit_events(created_at);

create trigger user_profile_set_updated_at
before update on public.user_profile
for each row execute function public.set_updated_at();

create trigger tenants_set_updated_at
before update on public.tenants
for each row execute function public.set_updated_at();

create trigger user_memberships_set_updated_at
before update on public.user_memberships
for each row execute function public.set_updated_at();

create trigger employee_profiles_set_updated_at
before update on public.employee_profiles
for each row execute function public.set_updated_at();

create trigger customer_identity_set_updated_at
before update on public.customer_identity
for each row execute function public.set_updated_at();

create trigger customer_tenant_profiles_set_updated_at
before update on public.customer_tenant_profiles
for each row execute function public.set_updated_at();

create trigger employee_invitations_set_updated_at
before update on public.employee_invitations
for each row execute function public.set_updated_at();

create trigger customer_invitations_set_updated_at
before update on public.customer_invitations
for each row execute function public.set_updated_at();

