create extension if not exists pgcrypto with schema extensions;
create extension if not exists citext with schema extensions;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create or replace function public.is_tenant_member(p_tenant_id uuid)
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

create or replace function public.is_tenant_owner(p_tenant_id uuid)
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

create or replace function public.is_tenant_employee_or_owner(p_tenant_id uuid)
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

