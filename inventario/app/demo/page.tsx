'use client'

import { Plus, Download, LogOut, Package, Search, Crown, Pencil, Trash2 } from 'lucide-react'

const demoProductos = [
  { id: '1', nombre: 'Coca-Cola Zero 500ml', categoria: 'Bebidas', codigo: '5449000131836', cantidad: 24, precio: 1.2, img: 'https://images.openfoodfacts.org/images/products/544/900/013/1836/front_es.130.400.jpg' },
  { id: '2', nombre: 'Nutella 400g', categoria: 'Alimentación', codigo: '3017620422003', cantidad: 8, precio: 3.49, img: 'https://images.openfoodfacts.org/images/products/301/762/042/2003/front_es.420.400.jpg' },
  { id: '3', nombre: 'Auriculares Bluetooth', categoria: 'Electrónica', codigo: '8412345678901', cantidad: 3, precio: 29.99, img: null },
  { id: '4', nombre: 'Aceite de oliva virgen 1L', categoria: 'Alimentación', codigo: '8410010812345', cantidad: 15, precio: 6.95, img: null },
]

export default function DemoDashboard() {
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
            <span className="text-xs font-bold px-2 py-1 rounded-full flex items-center gap-1 bg-blue-100 text-blue-700">
              <Crown className="w-3 h-3" /> PRO
            </span>
            <button className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-6 space-y-4">
        <div className="bg-white rounded-2xl p-4 border border-gray-100">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-500">4 / 500 productos</span>
            <button className="flex items-center gap-1 text-xs text-blue-600 font-medium hover:underline">
              <Download className="w-3 h-3" /> Exportar CSV
            </button>
          </div>
          <div className="w-full bg-gray-100 rounded-full h-1.5">
            <div className="h-1.5 rounded-full bg-blue-500" style={{ width: '2%' }} />
          </div>
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            readOnly
            placeholder="Buscar por nombre, categoría o código..."
            className="w-full pl-9 pr-4 py-3 bg-white border border-gray-200 rounded-xl text-sm"
          />
        </div>

        <div className="space-y-3">
          {demoProductos.map(p => (
            <div key={p.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex gap-3">
              {p.img ? (
                <img src={p.img} alt={p.nombre} className="w-16 h-16 object-cover rounded-lg flex-shrink-0" />
              ) : (
                <div className="w-16 h-16 bg-gray-100 rounded-lg flex items-center justify-center flex-shrink-0">
                  <Package className="w-7 h-7 text-gray-400" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-gray-900 truncate">{p.nombre}</h3>
                <span className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full">{p.categoria}</span>
                <p className="text-xs text-gray-400 mt-1">#{p.codigo}</p>
                <p className="text-sm text-green-600 font-medium">€{p.precio.toFixed(2)}</p>
                <div className="flex items-center gap-2 mt-2">
                  <span className="text-sm text-gray-600">Cantidad: <strong>{p.cantidad}</strong></span>
                  <button className="p-1 text-blue-600 hover:bg-blue-50 rounded"><Pencil className="w-4 h-4" /></button>
                  <button className="p-1 text-red-500 hover:bg-red-50 rounded"><Trash2 className="w-4 h-4" /></button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </main>

      <button className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 text-white rounded-full shadow-lg hover:bg-blue-700 flex items-center justify-center">
        <Plus className="w-7 h-7" />
      </button>
    </div>
  )
}
