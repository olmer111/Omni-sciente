import { createClient } from '@supabase/supabase-js'

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!

export const supabase = createClient(supabaseUrl, supabaseAnonKey)

export type Plan = 'gratuito' | 'pro' | 'max'

export interface Producto {
  id: string
  user_id: string
  nombre: string
  codigo_barras: string | null
  categoria: string | null
  cantidad: number
  precio: number | null
  imagen_url: string | null
  notas: string | null
  created_at: string
  updated_at: string
}

export interface Perfil {
  id: string
  email: string
  plan: Plan
  productos_count: number
}

export const LIMITES_PLAN = {
  gratuito: 30,
  pro: 500,
  max: Infinity,
}

export const PRECIOS_PLAN = {
  gratuito: 0,
  pro: 9.99,
  max: 24.99,
}
