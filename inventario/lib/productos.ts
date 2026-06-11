import { supabase, Producto, LIMITES_PLAN, Plan } from './supabase'

export async function obtenerProductos(userId: string): Promise<Producto[]> {
  const { data, error } = await supabase
    .from('productos')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false })

  if (error) throw error
  return data || []
}

export async function agregarProducto(
  userId: string,
  plan: Plan,
  producto: Omit<Producto, 'id' | 'user_id' | 'created_at' | 'updated_at'>
): Promise<Producto> {
  const limite = LIMITES_PLAN[plan]
  const { count } = await supabase
    .from('productos')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', userId)

  if (count !== null && count >= limite) {
    throw new Error(`Has alcanzado el límite de ${limite} productos para el plan ${plan}.`)
  }

  const { data, error } = await supabase
    .from('productos')
    .insert({ ...producto, user_id: userId })
    .select()
    .single()

  if (error) throw error
  return data
}

export async function actualizarProducto(
  id: string,
  cambios: Partial<Producto>
): Promise<Producto> {
  const { data, error } = await supabase
    .from('productos')
    .update({ ...cambios, updated_at: new Date().toISOString() })
    .eq('id', id)
    .select()
    .single()

  if (error) throw error
  return data
}

export async function eliminarProducto(id: string): Promise<void> {
  const { error } = await supabase.from('productos').delete().eq('id', id)
  if (error) throw error
}

export async function buscarPorCodigo(
  userId: string,
  codigo: string
): Promise<Producto | null> {
  const { data } = await supabase
    .from('productos')
    .select('*')
    .eq('user_id', userId)
    .eq('codigo_barras', codigo)
    .single()

  return data || null
}

export async function buscarInfoProducto(codigo: string): Promise<{
  nombre: string
  imagen_url?: string
} | null> {
  try {
    const res = await fetch(`https://world.openfoodfacts.org/api/v0/product/${codigo}.json`)
    const json = await res.json()
    if (json.status === 1 && json.product) {
      return {
        nombre: json.product.product_name || json.product.generic_name || '',
        imagen_url: json.product.image_url,
      }
    }
  } catch {}
  return null
}

export async function exportarCSV(productos: Producto[]): Promise<string> {
  const cabeceras = ['Nombre', 'Código', 'Categoría', 'Cantidad', 'Precio', 'Notas']
  const filas = productos.map(p => [
    p.nombre,
    p.codigo_barras || '',
    p.categoria || '',
    p.cantidad,
    p.precio || '',
    p.notas || '',
  ])
  return [cabeceras, ...filas].map(f => f.join(',')).join('\n')
}
