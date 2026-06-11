'use client'

import { useState } from 'react'
import { Producto } from '@/lib/supabase'
import { actualizarProducto, eliminarProducto } from '@/lib/productos'
import { Package, Pencil, Trash2, Check, X } from 'lucide-react'

interface ProductoCardProps {
  producto: Producto
  onActualizado: (p: Producto) => void
  onEliminado: (id: string) => void
}

export default function ProductoCard({ producto, onActualizado, onEliminado }: ProductoCardProps) {
  const [editando, setEditando] = useState(false)
  const [cantidad, setCantidad] = useState(producto.cantidad)
  const [cargando, setCargando] = useState(false)

  const guardar = async () => {
    setCargando(true)
    try {
      const actualizado = await actualizarProducto(producto.id, { cantidad })
      onActualizado(actualizado)
      setEditando(false)
    } finally {
      setCargando(false)
    }
  }

  const eliminar = async () => {
    if (!confirm(`¿Eliminar "${producto.nombre}"?`)) return
    setCargando(true)
    try {
      await eliminarProducto(producto.id)
      onEliminado(producto.id)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex gap-3">
      {producto.imagen_url ? (
        <img
          src={producto.imagen_url}
          alt={producto.nombre}
          className="w-16 h-16 object-cover rounded-lg flex-shrink-0"
        />
      ) : (
        <div className="w-16 h-16 bg-gray-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <Package className="w-7 h-7 text-gray-400" />
        </div>
      )}

      <div className="flex-1 min-w-0">
        <h3 className="font-semibold text-gray-900 truncate">{producto.nombre}</h3>
        {producto.categoria && (
          <span className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full">
            {producto.categoria}
          </span>
        )}
        {producto.codigo_barras && (
          <p className="text-xs text-gray-400 mt-1">#{producto.codigo_barras}</p>
        )}
        {producto.precio && (
          <p className="text-sm text-green-600 font-medium">€{producto.precio.toFixed(2)}</p>
        )}

        <div className="flex items-center gap-2 mt-2">
          {editando ? (
            <>
              <input
                type="number"
                min="0"
                value={cantidad}
                onChange={e => setCantidad(Number(e.target.value))}
                className="w-20 border rounded-lg px-2 py-1 text-sm"
              />
              <button
                onClick={guardar}
                disabled={cargando}
                className="p-1 text-green-600 hover:bg-green-50 rounded"
              >
                <Check className="w-4 h-4" />
              </button>
              <button
                onClick={() => { setEditando(false); setCantidad(producto.cantidad) }}
                className="p-1 text-gray-400 hover:bg-gray-50 rounded"
              >
                <X className="w-4 h-4" />
              </button>
            </>
          ) : (
            <>
              <span className="text-sm text-gray-600">
                Cantidad: <strong>{producto.cantidad}</strong>
              </span>
              <button
                onClick={() => setEditando(true)}
                className="p-1 text-blue-600 hover:bg-blue-50 rounded"
              >
                <Pencil className="w-4 h-4" />
              </button>
              <button
                onClick={eliminar}
                disabled={cargando}
                className="p-1 text-red-500 hover:bg-red-50 rounded"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
