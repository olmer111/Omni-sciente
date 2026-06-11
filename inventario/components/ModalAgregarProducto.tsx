'use client'

import { useState } from 'react'
import { X, ScanLine, Loader2 } from 'lucide-react'
import Scanner from './Scanner'
import { buscarInfoProducto } from '@/lib/productos'

interface ModalAgregarProductoProps {
  onGuardar: (producto: {
    nombre: string
    codigo_barras: string | null
    categoria: string | null
    cantidad: number
    precio: number | null
    imagen_url: string | null
    notas: string | null
  }) => void
  onClose: () => void
}

export default function ModalAgregarProducto({ onGuardar, onClose }: ModalAgregarProductoProps) {
  const [mostrarScanner, setMostrarScanner] = useState(false)
  const [buscando, setBuscando] = useState(false)
  const [form, setForm] = useState({
    nombre: '',
    codigo_barras: '',
    categoria: '',
    cantidad: 1,
    precio: '',
    imagen_url: '',
    notas: '',
  })

  const handleCodigo = async (codigo: string) => {
    setMostrarScanner(false)
    setBuscando(true)
    setForm(f => ({ ...f, codigo_barras: codigo }))

    const info = await buscarInfoProducto(codigo)
    if (info) {
      setForm(f => ({
        ...f,
        nombre: info.nombre || f.nombre,
        imagen_url: info.imagen_url || f.imagen_url,
      }))
    }
    setBuscando(false)
  }

  const handleGuardar = () => {
    if (!form.nombre.trim()) return
    onGuardar({
      nombre: form.nombre.trim(),
      codigo_barras: form.codigo_barras || null,
      categoria: form.categoria || null,
      cantidad: form.cantidad,
      precio: form.precio ? Number(form.precio) : null,
      imagen_url: form.imagen_url || null,
      notas: form.notas || null,
    })
  }

  return (
    <>
      {mostrarScanner && (
        <Scanner
          onDetected={handleCodigo}
          onClose={() => setMostrarScanner(false)}
        />
      )}

      <div className="fixed inset-0 z-40 bg-black/60 flex items-end sm:items-center justify-center p-4">
        <div className="bg-white rounded-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
          <div className="flex items-center justify-between p-4 border-b sticky top-0 bg-white">
            <h2 className="font-bold text-lg">Agregar producto</h2>
            <button onClick={onClose} className="p-1 rounded-full hover:bg-gray-100">
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-4 space-y-4">
            <button
              onClick={() => setMostrarScanner(true)}
              className="w-full flex items-center justify-center gap-2 border-2 border-dashed border-blue-300 rounded-xl p-4 text-blue-600 hover:bg-blue-50 transition"
            >
              <ScanLine className="w-5 h-5" />
              <span className="font-medium">Escanear código de barras / QR</span>
            </button>

            {buscando && (
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                Buscando información del producto...
              </div>
            )}

            {form.imagen_url && (
              <img
                src={form.imagen_url}
                alt="Producto"
                className="w-24 h-24 object-cover rounded-lg mx-auto"
              />
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Nombre <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={form.nombre}
                onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))}
                placeholder="Ej: Coca-Cola 500ml"
                className="w-full border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Cantidad</label>
                <input
                  type="number"
                  min="0"
                  value={form.cantidad}
                  onChange={e => setForm(f => ({ ...f, cantidad: Number(e.target.value) }))}
                  className="w-full border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Precio (€)</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.precio}
                  onChange={e => setForm(f => ({ ...f, precio: e.target.value }))}
                  placeholder="0.00"
                  className="w-full border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Categoría</label>
              <input
                type="text"
                value={form.categoria}
                onChange={e => setForm(f => ({ ...f, categoria: e.target.value }))}
                placeholder="Ej: Bebidas, Electrónica..."
                className="w-full border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Código de barras</label>
              <input
                type="text"
                value={form.codigo_barras}
                onChange={e => setForm(f => ({ ...f, codigo_barras: e.target.value }))}
                placeholder="Se rellena al escanear"
                className="w-full border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Notas</label>
              <textarea
                value={form.notas}
                onChange={e => setForm(f => ({ ...f, notas: e.target.value }))}
                rows={2}
                placeholder="Información adicional..."
                className="w-full border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
            </div>

            <button
              onClick={handleGuardar}
              disabled={!form.nombre.trim()}
              className="w-full bg-blue-600 text-white font-semibold py-3 rounded-xl hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              Guardar producto
            </button>
          </div>
        </div>
      </div>
    </>
  )
}
