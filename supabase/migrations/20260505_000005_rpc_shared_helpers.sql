create or replace function private.require_authenticated()
returns uuid
language plpgsql
stable
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
begin
  v_user_id := auth.uid();

  if v_user_id is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  return v_user_id;
end;
$$;

create or replace function private.current_user_email()
returns text
language plpgsql
stable
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_email text;
begin
  perform private.require_authenticated();

  v_email := nullif(auth.jwt() ->> 'email', '');

  if v_email is null then
    raise exception 'missing_auth_email' using errcode = '22023';
  end if;

  return lower(v_email);
end;
$$;

create or replace function private.new_invite_token()
returns text
language sql
volatile
security definer
set search_path = public, private, pg_temp
as $$
  select encode(extensions.gen_random_bytes(32), 'hex');
$$;

create or replace function private.hash_invite_token(p_token text)
returns text
language sql
immutable
security definer
set search_path = public, private, pg_temp
as $$
  select encode(extensions.digest(coalesce(p_token, ''), 'sha256'), 'hex');
$$;

create or replace function private.audit_event(
  p_event_type text,
  p_tenant_id uuid default null,
  p_event_payload jsonb default '{}'::jsonb
)
returns void
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
begin
  insert into public.audit_events (
    actor_user_id,
    tenant_id,
    event_type,
    event_payload
  )
  values (
    auth.uid(),
    p_tenant_id,
    p_event_type,
    coalesce(p_event_payload, '{}'::jsonb)
  );
end;
$$;

create or replace function private.context_json(
  p_context_id text,
  p_role text,
  p_tenant_id uuid,
  p_tenant_name text,
  p_membership_id uuid,
  p_employee_profile_id uuid default null,
  p_customer_identity_id uuid default null,
  p_customer_tenant_profile_id uuid default null,
  p_status text default 'active',
  p_created_at timestamptz default now()
)
returns jsonb
language sql
stable
security definer
set search_path = public, private, pg_temp
as $$
  select jsonb_build_object(
    'context_id', p_context_id,
    'role', p_role,
    'tenant_id', p_tenant_id,
    'tenant_name', p_tenant_name,
    'membership_id', p_membership_id,
    'employee_profile_id', p_employee_profile_id,
    'customer_identity_id', p_customer_identity_id,
    'customer_tenant_profile_id', p_customer_tenant_profile_id,
    'status', p_status,
    'created_at', p_created_at
  );
$$;

grant usage on schema private to authenticated;
grant execute on function private.require_authenticated() to authenticated;
grant execute on function private.current_user_email() to authenticated;
grant execute on function private.new_invite_token() to authenticated;
grant execute on function private.hash_invite_token(text) to authenticated;
grant execute on function private.audit_event(text, uuid, jsonb) to authenticated;
grant execute on function private.context_json(text, text, uuid, text, uuid, uuid, uuid, uuid, text, timestamptz) to authenticated;

