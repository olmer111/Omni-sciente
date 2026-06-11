'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { supabase, Producto, Plan, LIMITES_PLAN } from '@/lib/supabase'
import { obtenerProductos, agregarProducto, exportarCSV } from '@/lib/productos'
import ProductoCard from '@/components/ProductoCard'
import ModalAgregarProducto from '@/components/ModalAgregarProducto'
import { Plus, Download, LogOut, Package, Search, Crown, Loader2 } from 'lucide-react'

export default function Dashboard() {
  const router = useRouter()
  const [userId, setUserId] = useState<string | null>(null)
  const [plan, setPlan] = useState<Plan>('gratuito')
  const [productos, setProductos] = useState<Producto[]>([])
  const [busqueda, setBusqueda] = useState('')
  const [cargando, setCargando] = useState(true)
  const [mostrarModal, setMostrarModal] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const init = async () => {
      const { data: { user } } = await supabase.auth.getUser()
      if (!user) { router.push('/auth/login'); return }

      setUserId(user.id)

      const { data: perfil } = await supabase
        .from('perfiles')
        .select('plan')
        .eq('id', user.id)
        .single()

      if (perfil) setPlan(perfil.plan as Plan)

      const lista = await obtenerProductos(user.id)
      setProductos(lista)
      setCargando(false)
    }
    init()
  }, [router])

  const handleAgregarProducto = async (datos: Omit<Producto, 'id' | 'user_id' | 'created_at' | 'updated_at'>) => {
    if (!userId) return
    setError('')
    try {
      const nuevo = await agregarProducto(userId, plan, datos)
      setProductos(prev => [nuevo, ...prev])
      setMostrarModal(false)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const handleExportar = async () => {
    if (plan === 'gratuito') {
      alert('La exportación CSV está disponible en los planes Pro y Max.')
      return
    }
    const csv = await exportarCSV(productos)
    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'inventario.csv'
    a.click()
  }

  const handleLogout = async () => {
    await supabase.auth.signOut()
    router.push('/')
  }

  const productosFiltrados = productos.filter(p =>
    p.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    (p.categoria && p.categoria.toLowerCase().includes(busqueda.toLowerCase())) ||
    (p.codigo_barras && p.codigo_barras.includes(busqueda))
  )

  const limite = LIMITES_PLAN[plan]
  const porcentaje = limite === Infinity ? 0 : Math.round((productos.length / limite) * 100)

  const planColors: Record<Plan, string> = {
    gratuito: 'bg-gray-100 text-gray-700',
    pro: 'bg-blue-100 text-blue-700',
    max: 'bg-purple-100 text-purple-700',
  }

  if (cargando) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <Package className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-gray-900">StockScan</span>
          </div>
          <div className="flex items-center gap-2">
            <span className={`text-xs font-bold px-2 py-1 rounded-full flex items-center gap-1 ${planColors[plan]}`}>
              {plan !== 'gratuito' && <Crown className="w-3 h-3" />}
              {plan.toUpperCase()}
            </span>
            <button onClick={handleLogout} className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-6 space-y-4">
        {/* Stats */}
        <div className="bg-white rounded-2xl p-4 border border-gray-100">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-500">
              {productos.length} / {limite === Infinity ? '∞' : limite} productos
            </span>
            {plan !== 'gratuito' && (
              <button onClick={handleExportar} className="flex items-center gap-1 text-xs text-blue-600 font-medium hover:underline">
                <Download className="w-3 h-3" />
                Exportar CSV
              </button>
            )}
          </div>
          {limite !== Infinity && (
            <div className="w-full bg-gray-100 rounded-full h-1.5">
              <div
                className={`h-1.5 rounded-full ${porcentaje > 80 ? 'bg-red-500' : 'bg-blue-500'}`}
                style={{ width: `${porcentaje}%` }}
              />
            </div>
          )}
          {plan === 'gratuito' && productos.length >= 20 && (
            <p className="text-xs text-amber-600 mt-2">
              Te quedan {30 - productos.length} productos en el plan gratuito.{' '}
              <a href="/pricing" className="underline font-medium">Actualizar plan</a>
            </p>
          )}
        </div>

        {error && (
          <div className="bg-red-50 text-red-600 text-sm p-3 rounded-xl">{error}</div>
        )}

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre, categoría o código..."
            className="w-full pl-9 pr-4 py-3 bg-white border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* Products */}
        <div className="space-y-3">
          {productosFiltrados.length === 0 ? (
            <div className="text-center py-16 text-gray-400">
              <Package className="w-12 h-12 mx-auto mb-3 opacity-30" />
              <p className="font-medium">
                {busqueda ? 'Sin resultados' : 'Tu inventario está vacío'}
              </p>
              <p className="text-sm mt-1">
                {busqueda ? 'Prueba con otros términos' : 'Pulsa + para agregar tu primer producto'}
              </p>
            </div>
          ) : (
            productosFiltrados.map(p => (
              <ProductoCard
                key={p.id}
                producto={p}
                onActualizado={actualizado =>
                  setProductos(prev => prev.map(x => x.id === actualizado.id ? actualizado : x))
                }
                onEliminado={id =>
                  setProductos(prev => prev.filter(x => x.id !== id))
                }
              />
            ))
          )}
        </div>
      </main>

      {/* FAB */}
      <button
        onClick={() => setMostrarModal(true)}
        className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 text-white rounded-full shadow-lg hover:bg-blue-700 flex items-center justify-center"
      >
        <Plus className="w-7 h-7" />
      </button>

      {mostrarModal && (
        <ModalAgregarProducto
          onGuardar={handleAgregarProducto}
          onClose={() => setMostrarModal(false)}
        />
      )}
    </div>
  )
}
