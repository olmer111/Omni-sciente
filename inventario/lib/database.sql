-- Ejecutar en Supabase SQL Editor

-- Tabla de perfiles (extiende auth.users)
create table public.perfiles (
  id uuid references auth.users on delete cascade primary key,
  email text not null,
  plan text not null default 'gratuito' check (plan in ('gratuito', 'pro', 'max')),
  created_at timestamptz default now()
);

-- Tabla de productos
create table public.productos (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references public.perfiles(id) on delete cascade not null,
  nombre text not null,
  codigo_barras text,
  categoria text,
  cantidad integer not null default 1,
  precio numeric(10,2),
  imagen_url text,
  notas text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- Índices
create index productos_user_id_idx on public.productos(user_id);
create index productos_codigo_barras_idx on public.productos(codigo_barras);

-- Row Level Security
alter table public.perfiles enable row level security;
alter table public.productos enable row level security;

create policy "Usuarios ven su perfil" on public.perfiles
  for all using (auth.uid() = id);

create policy "Usuarios gestionan sus productos" on public.productos
  for all using (auth.uid() = user_id);

-- Trigger: crear perfil al registrarse
create or replace function public.crear_perfil_usuario()
returns trigger language plpgsql security definer as $$
begin
  insert into public.perfiles (id, email)
  values (new.id, new.email);
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.crear_perfil_usuario();

-- Función: contar productos del usuario
create or replace function public.contar_productos(user_uuid uuid)
returns integer language sql security definer as $$
  select count(*)::integer from public.productos where user_id = user_uuid;
$$;
