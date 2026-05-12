create or replace function private.create_owner_tenant_impl(
  p_name text,
  p_industry text default null,
  p_country text default null,
  p_timezone text default null,
  p_email text default null,
  p_phone text default null
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_tenant public.tenants;
  v_membership public.user_memberships;
begin
  v_user_id := private.require_authenticated();
  perform private.ensure_user_profile_impl(null, null, p_timezone);

  if nullif(trim(p_name), '') is null then
    raise exception 'tenant_name_required' using errcode = '22023';
  end if;

  insert into public.tenants (
    name,
    industry,
    country,
    timezone,
    email,
    phone
  )
  values (
    trim(p_name),
    nullif(trim(p_industry), ''),
    nullif(trim(p_country), ''),
    nullif(trim(p_timezone), ''),
    nullif(trim(p_email), ''),
    nullif(trim(p_phone), '')
  )
  returning * into v_tenant;

  insert into public.user_memberships (
    user_id,
    tenant_id,
    role,
    status,
    role_label,
    assigned_by
  )
  values (
    v_user_id,
    v_tenant.id,
    'owner',
    'active',
    'Owner',
    v_user_id
  )
  returning * into v_membership;

  insert into public.tenant_key_registry (
    tenant_id,
    key_ref,
    key_version
  )
  values (
    v_tenant.id,
    'tenant:' || v_tenant.id::text || ':v1',
    1
  );

  perform private.audit_event(
    'owner_tenant_created',
    v_tenant.id,
    jsonb_build_object('tenant_name', v_tenant.name)
  );

  return private.context_json(
    'owner:' || v_tenant.id::text,
    'owner',
    v_tenant.id,
    v_tenant.name,
    v_membership.id,
    null,
    null,
    null,
    v_membership.status,
    v_membership.created_at
  );
end;
$$;

create or replace function public.create_owner_tenant(
  p_name text,
  p_industry text default null,
  p_country text default null,
  p_timezone text default null,
  p_email text default null,
  p_phone text default null
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.create_owner_tenant_impl(p_name, p_industry, p_country, p_timezone, p_email, p_phone);
$$;

create or replace function private.ensure_customer_identity_impl(
  p_first_name text,
  p_last_name text,
  p_phone text default null,
  p_avatar_url text default null
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_email text;
  v_customer public.customer_identity;
begin
  v_user_id := private.require_authenticated();
  v_email := private.current_user_email();
  perform private.ensure_user_profile_impl(null, p_phone, null);

  if nullif(trim(p_first_name), '') is null then
    raise exception 'first_name_required' using errcode = '22023';
  end if;

  if p_last_name is null then
    p_last_name := '';
  end if;

  insert into public.customer_identity (
    user_id,
    email,
    first_name,
    last_name,
    phone,
    avatar_url,
    status
  )
  values (
    v_user_id,
    v_email,
    trim(p_first_name),
    trim(p_last_name),
    nullif(trim(p_phone), ''),
    nullif(trim(p_avatar_url), ''),
    'active'
  )
  on conflict (user_id) do update
  set
    email = excluded.email,
    first_name = excluded.first_name,
    last_name = excluded.last_name,
    phone = coalesce(excluded.phone, public.customer_identity.phone),
    avatar_url = coalesce(excluded.avatar_url, public.customer_identity.avatar_url),
    status = 'active'
  returning * into v_customer;

  perform private.audit_event(
    'customer_identity_ensured',
    null,
    jsonb_build_object('customer_identity_id', v_customer.id)
  );

  return jsonb_build_object(
    'customer_identity_id', v_customer.id,
    'user_id', v_customer.user_id,
    'email', v_customer.email,
    'first_name', v_customer.first_name,
    'last_name', v_customer.last_name,
    'phone', v_customer.phone,
    'status', v_customer.status
  );
end;
$$;

create or replace function public.ensure_customer_identity(
  p_first_name text,
  p_last_name text,
  p_phone text default null,
  p_avatar_url text default null
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.ensure_customer_identity_impl(p_first_name, p_last_name, p_phone, p_avatar_url);
$$;

create or replace function private.create_employee_invitation_impl(
  p_tenant_id uuid,
  p_email text,
  p_full_name text default null,
  p_job_title text default null
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_token text;
  v_invitation public.employee_invitations;
begin
  v_user_id := private.require_authenticated();

  if not private.is_tenant_owner(p_tenant_id) then
    raise exception 'not_tenant_owner' using errcode = '42501';
  end if;

  if nullif(trim(p_email), '') is null then
    raise exception 'invite_email_required' using errcode = '22023';
  end if;

  update public.employee_invitations
  set status = 'cancelled',
      cancelled_at = now()
  where tenant_id = p_tenant_id
    and lower(email::text) = lower(trim(p_email))
    and status = 'pending';

  v_token := private.new_invite_token();

  insert into public.employee_invitations (
    tenant_id,
    email,
    token_hash,
    invited_by,
    suggested_full_name,
    suggested_job_title,
    expires_at
  )
  values (
    p_tenant_id,
    lower(trim(p_email)),
    private.hash_invite_token(v_token),
    v_user_id,
    nullif(trim(p_full_name), ''),
    nullif(trim(p_job_title), ''),
    now() + interval '7 days'
  )
  returning * into v_invitation;

  perform private.audit_event(
    'employee_invitation_created',
    p_tenant_id,
    jsonb_build_object('invitation_id', v_invitation.id, 'email', v_invitation.email)
  );

  return jsonb_build_object(
    'invitation_id', v_invitation.id,
    'tenant_id', v_invitation.tenant_id,
    'email', v_invitation.email,
    'token', v_token,
    'invite_url', 'nexora://employee-invite/' || v_token,
    'expires_at', v_invitation.expires_at
  );
end;
$$;

create or replace function public.create_employee_invitation(
  p_tenant_id uuid,
  p_email text,
  p_full_name text default null,
  p_job_title text default null
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.create_employee_invitation_impl(p_tenant_id, p_email, p_full_name, p_job_title);
$$;

create or replace function private.create_customer_invitation_impl(
  p_tenant_id uuid,
  p_email text,
  p_first_name text default null,
  p_last_name text default null
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_token text;
  v_invitation public.customer_invitations;
begin
  v_user_id := private.require_authenticated();

  if not private.is_tenant_owner(p_tenant_id) then
    raise exception 'not_tenant_owner' using errcode = '42501';
  end if;

  if nullif(trim(p_email), '') is null then
    raise exception 'invite_email_required' using errcode = '22023';
  end if;

  update public.customer_invitations
  set status = 'cancelled',
      cancelled_at = now()
  where tenant_id = p_tenant_id
    and lower(email::text) = lower(trim(p_email))
    and status = 'pending';

  v_token := private.new_invite_token();

  insert into public.customer_invitations (
    tenant_id,
    email,
    token_hash,
    invited_by,
    suggested_first_name,
    suggested_last_name,
    expires_at
  )
  values (
    p_tenant_id,
    lower(trim(p_email)),
    private.hash_invite_token(v_token),
    v_user_id,
    nullif(trim(p_first_name), ''),
    nullif(trim(p_last_name), ''),
    now() + interval '7 days'
  )
  returning * into v_invitation;

  perform private.audit_event(
    'customer_invitation_created',
    p_tenant_id,
    jsonb_build_object('invitation_id', v_invitation.id, 'email', v_invitation.email)
  );

  return jsonb_build_object(
    'invitation_id', v_invitation.id,
    'tenant_id', v_invitation.tenant_id,
    'email', v_invitation.email,
    'token', v_token,
    'invite_url', 'nexora://customer-invite/' || v_token,
    'expires_at', v_invitation.expires_at
  );
end;
$$;

create or replace function public.create_customer_invitation(
  p_tenant_id uuid,
  p_email text,
  p_first_name text default null,
  p_last_name text default null
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.create_customer_invitation_impl(p_tenant_id, p_email, p_first_name, p_last_name);
$$;

create or replace function private.accept_employee_invitation_impl(
  p_token text,
  p_full_name text default null,
  p_phone text default null
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_email text;
  v_invitation public.employee_invitations;
  v_tenant public.tenants;
  v_membership public.user_memberships;
  v_profile public.employee_profiles;
begin
  v_user_id := private.require_authenticated();
  v_email := private.current_user_email();
  perform private.ensure_user_profile_impl(null, p_phone, null);

  select * into v_invitation
  from public.employee_invitations
  where token_hash = private.hash_invite_token(p_token)
  for update;

  if not found then
    raise exception 'invalid_invitation' using errcode = '22023';
  end if;

  if v_invitation.status = 'accepted' and v_invitation.accepted_by_user_id = v_user_id then
    select * into v_membership
    from public.user_memberships
    where user_id = v_user_id
      and tenant_id = v_invitation.tenant_id
      and role = 'employee';

    select * into v_profile
    from public.employee_profiles
    where user_id = v_user_id
      and tenant_id = v_invitation.tenant_id;

    select * into v_tenant from public.tenants where id = v_invitation.tenant_id;

    return private.context_json(
      'employee:' || v_profile.id::text,
      'employee',
      v_tenant.id,
      v_tenant.name,
      v_membership.id,
      v_profile.id
    );
  end if;

  if v_invitation.status <> 'pending' then
    raise exception 'invitation_not_pending' using errcode = '22023';
  end if;

  if v_invitation.expires_at < now() then
    update public.employee_invitations
    set status = 'expired'
    where id = v_invitation.id;
    raise exception 'invitation_expired' using errcode = '22023';
  end if;

  if lower(v_invitation.email::text) <> lower(v_email) then
    raise exception 'invite_email_mismatch' using errcode = '42501';
  end if;

  select * into v_tenant
  from public.tenants
  where id = v_invitation.tenant_id
    and status = 'active';

  if not found then
    raise exception 'tenant_not_active' using errcode = '22023';
  end if;

  insert into public.user_memberships (
    user_id,
    tenant_id,
    role,
    status,
    role_label,
    assigned_by
  )
  values (
    v_user_id,
    v_invitation.tenant_id,
    'employee',
    'active',
    'Employee',
    v_invitation.invited_by
  )
  on conflict (user_id, tenant_id, role) do update
  set status = 'active',
      assigned_by = excluded.assigned_by,
      assigned_at = now()
  returning * into v_membership;

  insert into public.employee_profiles (
    user_id,
    tenant_id,
    membership_id,
    full_name,
    email,
    phone,
    job_title,
    status,
    invited_by,
    invitation_accepted_at
  )
  values (
    v_user_id,
    v_invitation.tenant_id,
    v_membership.id,
    coalesce(nullif(trim(p_full_name), ''), v_invitation.suggested_full_name, v_email),
    v_email,
    nullif(trim(p_phone), ''),
    v_invitation.suggested_job_title,
    'active',
    v_invitation.invited_by,
    now()
  )
  on conflict (user_id, tenant_id) do update
  set membership_id = excluded.membership_id,
      full_name = excluded.full_name,
      phone = coalesce(excluded.phone, public.employee_profiles.phone),
      job_title = coalesce(excluded.job_title, public.employee_profiles.job_title),
      status = 'active',
      invitation_accepted_at = now()
  returning * into v_profile;

  update public.employee_invitations
  set status = 'accepted',
      accepted_by_user_id = v_user_id,
      accepted_at = now()
  where id = v_invitation.id;

  perform private.audit_event(
    'employee_invitation_accepted',
    v_invitation.tenant_id,
    jsonb_build_object('invitation_id', v_invitation.id, 'employee_profile_id', v_profile.id)
  );

  return private.context_json(
    'employee:' || v_profile.id::text,
    'employee',
    v_tenant.id,
    v_tenant.name,
    v_membership.id,
    v_profile.id,
    null,
    null,
    v_membership.status,
    v_membership.created_at
  );
end;
$$;

create or replace function public.accept_employee_invitation(
  p_token text,
  p_full_name text default null,
  p_phone text default null
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.accept_employee_invitation_impl(p_token, p_full_name, p_phone);
$$;

create or replace function private.accept_customer_invitation_impl(
  p_token text,
  p_profile jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_user_id uuid;
  v_email text;
  v_invitation public.customer_invitations;
  v_tenant public.tenants;
  v_membership public.user_memberships;
  v_customer public.customer_identity;
  v_customer_profile public.customer_tenant_profiles;
  v_first_name text;
  v_last_name text;
begin
  v_user_id := private.require_authenticated();
  v_email := private.current_user_email();
  perform private.ensure_user_profile_impl(null, p_profile ->> 'phone', null);

  select * into v_invitation
  from public.customer_invitations
  where token_hash = private.hash_invite_token(p_token)
  for update;

  if not found then
    raise exception 'invalid_invitation' using errcode = '22023';
  end if;

  if v_invitation.status = 'accepted' and v_invitation.accepted_by_user_id = v_user_id then
    select * into v_customer
    from public.customer_identity
    where user_id = v_user_id;

    select * into v_membership
    from public.user_memberships
    where user_id = v_user_id
      and tenant_id = v_invitation.tenant_id
      and role = 'customer';

    select * into v_customer_profile
    from public.customer_tenant_profiles
    where customer_identity_id = v_customer.id
      and tenant_id = v_invitation.tenant_id;

    select * into v_tenant from public.tenants where id = v_invitation.tenant_id;

    return private.context_json(
      'customer:' || v_customer_profile.id::text,
      'customer',
      v_tenant.id,
      v_tenant.name,
      v_membership.id,
      null,
      v_customer.id,
      v_customer_profile.id
    );
  end if;

  if v_invitation.status <> 'pending' then
    raise exception 'invitation_not_pending' using errcode = '22023';
  end if;

  if v_invitation.expires_at < now() then
    update public.customer_invitations
    set status = 'expired'
    where id = v_invitation.id;
    raise exception 'invitation_expired' using errcode = '22023';
  end if;

  if lower(v_invitation.email::text) <> lower(v_email) then
    raise exception 'invite_email_mismatch' using errcode = '42501';
  end if;

  select * into v_tenant
  from public.tenants
  where id = v_invitation.tenant_id
    and status = 'active';

  if not found then
    raise exception 'tenant_not_active' using errcode = '22023';
  end if;

  v_first_name := coalesce(
    nullif(trim(p_profile ->> 'first_name'), ''),
    v_invitation.suggested_first_name,
    split_part(v_email, '@', 1)
  );
  v_last_name := coalesce(
    nullif(trim(p_profile ->> 'last_name'), ''),
    v_invitation.suggested_last_name,
    ''
  );

  insert into public.customer_identity (
    user_id,
    email,
    first_name,
    last_name,
    phone,
    status
  )
  values (
    v_user_id,
    v_email,
    v_first_name,
    v_last_name,
    nullif(trim(p_profile ->> 'phone'), ''),
    'active'
  )
  on conflict (user_id) do update
  set email = excluded.email,
      first_name = excluded.first_name,
      last_name = excluded.last_name,
      phone = coalesce(excluded.phone, public.customer_identity.phone),
      status = 'active'
  returning * into v_customer;

  insert into public.user_memberships (
    user_id,
    tenant_id,
    role,
    status,
    role_label,
    assigned_by
  )
  values (
    v_user_id,
    v_invitation.tenant_id,
    'customer',
    'active',
    'Customer',
    v_invitation.invited_by
  )
  on conflict (user_id, tenant_id, role) do update
  set status = 'active',
      assigned_by = excluded.assigned_by,
      assigned_at = now()
  returning * into v_membership;

  insert into public.customer_tenant_profiles (
    customer_identity_id,
    tenant_id,
    membership_id,
    relationship_status,
    encrypted_profile_blob,
    encryption_key_ref,
    encryption_key_version,
    encrypted_at
  )
  values (
    v_customer.id,
    v_invitation.tenant_id,
    v_membership.id,
    'active',
    convert_to(coalesce(p_profile, '{}'::jsonb)::text, 'UTF8'),
    'tenant:' || v_invitation.tenant_id::text || ':v1',
    1,
    now()
  )
  on conflict (customer_identity_id, tenant_id) do update
  set membership_id = excluded.membership_id,
      relationship_status = 'active',
      encrypted_profile_blob = excluded.encrypted_profile_blob,
      encryption_key_ref = excluded.encryption_key_ref,
      encryption_key_version = excluded.encryption_key_version,
      encrypted_at = excluded.encrypted_at
  returning * into v_customer_profile;

  update public.customer_invitations
  set status = 'accepted',
      accepted_by_user_id = v_user_id,
      accepted_customer_identity_id = v_customer.id,
      accepted_at = now()
  where id = v_invitation.id;

  perform private.audit_event(
    'customer_invitation_accepted',
    v_invitation.tenant_id,
    jsonb_build_object('invitation_id', v_invitation.id, 'customer_tenant_profile_id', v_customer_profile.id)
  );

  return private.context_json(
    'customer:' || v_customer_profile.id::text,
    'customer',
    v_tenant.id,
    v_tenant.name,
    v_membership.id,
    null,
    v_customer.id,
    v_customer_profile.id,
    v_membership.status,
    v_membership.created_at
  );
end;
$$;

create or replace function public.accept_customer_invitation(
  p_token text,
  p_profile jsonb default '{}'::jsonb
)
returns jsonb
language sql
volatile
set search_path = public, private, pg_temp
as $$
  select private.accept_customer_invitation_impl(p_token, p_profile);
$$;

revoke all on function public.create_owner_tenant(text, text, text, text, text, text) from public;
revoke all on function public.create_owner_tenant(text, text, text, text, text, text) from anon;
grant execute on function public.create_owner_tenant(text, text, text, text, text, text) to authenticated;

revoke all on function public.ensure_customer_identity(text, text, text, text) from public;
revoke all on function public.ensure_customer_identity(text, text, text, text) from anon;
grant execute on function public.ensure_customer_identity(text, text, text, text) to authenticated;

revoke all on function public.create_employee_invitation(uuid, text, text, text) from public;
revoke all on function public.create_employee_invitation(uuid, text, text, text) from anon;
grant execute on function public.create_employee_invitation(uuid, text, text, text) to authenticated;

revoke all on function public.create_customer_invitation(uuid, text, text, text) from public;
revoke all on function public.create_customer_invitation(uuid, text, text, text) from anon;
grant execute on function public.create_customer_invitation(uuid, text, text, text) to authenticated;

revoke all on function public.accept_employee_invitation(text, text, text) from public;
revoke all on function public.accept_employee_invitation(text, text, text) from anon;
grant execute on function public.accept_employee_invitation(text, text, text) to authenticated;

revoke all on function public.accept_customer_invitation(text, jsonb) from public;
revoke all on function public.accept_customer_invitation(text, jsonb) from anon;
grant execute on function public.accept_customer_invitation(text, jsonb) to authenticated;

grant execute on function private.create_owner_tenant_impl(text, text, text, text, text, text) to authenticated;
grant execute on function private.ensure_customer_identity_impl(text, text, text, text) to authenticated;
grant execute on function private.create_employee_invitation_impl(uuid, text, text, text) to authenticated;
grant execute on function private.create_customer_invitation_impl(uuid, text, text, text) to authenticated;
grant execute on function private.accept_employee_invitation_impl(text, text, text) to authenticated;
grant execute on function private.accept_customer_invitation_impl(text, jsonb) to authenticated;

