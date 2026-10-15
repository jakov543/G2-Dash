-- G2 Dash: run this once in Supabase → SQL Editor → New query → Run

-- 1) One row of synced data per user
create table if not exists public.user_data (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  data       jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);
alter table public.user_data enable row level security;

drop policy if exists "read own"   on public.user_data;
drop policy if exists "insert own" on public.user_data;
drop policy if exists "update own" on public.user_data;
drop policy if exists "delete own" on public.user_data;
create policy "read own"   on public.user_data for select using (auth.uid() = user_id);
create policy "insert own" on public.user_data for insert with check (auth.uid() = user_id);
create policy "update own" on public.user_data for update using (auth.uid() = user_id);
create policy "delete own" on public.user_data for delete using (auth.uid() = user_id);

-- 2) Lets a signed-in user delete their own account (required by the App Store and Google Play)
create or replace function public.delete_user()
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  delete from public.user_data where user_id = auth.uid();
  delete from auth.users where id = auth.uid();
end;
$$;
revoke all on function public.delete_user() from public, anon;
grant execute on function public.delete_user() to authenticated;
