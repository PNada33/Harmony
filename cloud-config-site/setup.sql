create table if not exists public.configs (
  id text primary key,
  name text not null,
  payload jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists configs_touch_updated_at on public.configs;
create trigger configs_touch_updated_at
before update on public.configs
for each row execute procedure public.touch_updated_at();

alter table public.configs enable row level security;

drop policy if exists "demo_anonymous_full_access" on public.configs;
create policy "demo_anonymous_full_access"
on public.configs
for all
to anon
using (true)
with check (true);
