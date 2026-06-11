import Link from 'next/link'
import { ScanLine, Package, BarChart3, Download, Shield } from 'lucide-react'

const planes = [
  {
    nombre: 'Gratuito',
    precio: '€0',
    periodo: 'para siempre',
    color: 'border-gray-200',
    botonColor: 'bg-gray-900 hover:bg-gray-800',
    caracteristicas: [
      'Hasta 30 productos',
      'Escaneo de códigos de barras',
      'Búsqueda automática de productos',
      'Gestión básica de inventario',
    ],
    limitaciones: ['Sin exportar CSV', 'Sin categorías avanzadas', 'Sin estadísticas'],
    href: '/auth/register?plan=gratuito',
  },
  {
    nombre: 'Pro',
    precio: '€9.99',
    periodo: '/mes',
    color: 'border-blue-500 ring-2 ring-blue-500',
    botonColor: 'bg-blue-600 hover:bg-blue-700',
    badge: 'MÁS POPULAR',
    limitaciones: [] as string[],
    caracteristicas: [
      'Hasta 500 productos',
      'Todo lo del plan Gratuito',
      'Exportar inventario en CSV',
      'Categorías y filtros',
      'Búsqueda avanzada',
      'Historial de cambios',
    ],
    href: '/auth/register?plan=pro',
  },
  {
    nombre: 'Max',
    precio: '€24.99',
    periodo: '/mes',
    color: 'border-purple-500',
    botonColor: 'bg-purple-600 hover:bg-purple-700',
    limitaciones: [] as string[],
    caracteristicas: [
      'Productos ilimitados',
      'Todo lo del plan Pro',
      'Múltiples almacenes',
      'Estadísticas y gráficas',
      'Acceso para 3 usuarios',
      'Soporte prioritario',
    ],
    href: '/auth/register?plan=max',
  },
]

export default function Home() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50">
      <header className="bg-white border-b sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <Package className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-xl text-gray-900">StockScan</span>
          </div>
          <div className="flex gap-3">
            <Link href="/auth/login" className="text-sm font-medium text-gray-600 hover:text-gray-900 px-3 py-2">
              Iniciar sesión
            </Link>
            <Link href="/auth/register" className="text-sm font-semibold bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
              Empezar gratis
            </Link>
          </div>
        </div>
      </header>

      <section className="max-w-5xl mx-auto px-4 py-20 text-center">
        <span className="bg-blue-100 text-blue-700 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wide">
          Inventario inteligente
        </span>
        <h1 className="mt-4 text-4xl sm:text-5xl font-extrabold text-gray-900 leading-tight">
          Escanea un producto.<br />
          <span className="text-blue-600">Tenlo en tu inventario.</span>
        </h1>
        <p className="mt-4 text-xl text-gray-500 max-w-xl mx-auto">
          Fotografía o escanea el código de barras de cualquier producto y organiza tu stock en segundos.
        </p>
        <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
          <Link href="/auth/register" className="bg-blue-600 text-white font-bold px-8 py-4 rounded-xl hover:bg-blue-700 text-lg">
            Empezar gratis →
          </Link>
          <Link href="#precios" className="border border-gray-300 text-gray-700 font-semibold px-8 py-4 rounded-xl hover:bg-gray-50 text-lg">
            Ver precios
          </Link>
        </div>
      </section>

      <section className="max-w-5xl mx-auto px-4 py-16">
        <div className="grid sm:grid-cols-3 gap-6">
          {[
            { icon: ScanLine, title: 'Escanea en segundos', desc: 'Apunta la cámara al código de barras o QR y el producto se añade automáticamente.' },
            { icon: BarChart3, title: 'Control total del stock', desc: 'Actualiza cantidades, precios y categorías desde cualquier dispositivo.' },
            { icon: Download, title: 'Exporta tu inventario', desc: 'Descarga tu stock en CSV para usarlo en Excel u otras herramientas. (Pro y Max)' },
          ].map(f => (
            <div key={f.title} className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
              <div className="w-12 h-12 bg-blue-50 rounded-xl flex items-center justify-center mb-4">
                <f.icon className="w-6 h-6 text-blue-600" />
              </div>
              <h3 className="font-bold text-gray-900 mb-2">{f.title}</h3>
              <p className="text-gray-500 text-sm">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="precios" className="max-w-5xl mx-auto px-4 py-16">
        <h2 className="text-3xl font-extrabold text-center text-gray-900 mb-2">Precios simples y transparentes</h2>
        <p className="text-center text-gray-500 mb-10">Empieza gratis. Escala cuando lo necesites.</p>
        <div className="grid sm:grid-cols-3 gap-6">
          {planes.map(plan => (
            <div key={plan.nombre} className={`bg-white rounded-2xl p-6 border-2 ${plan.color} relative`}>
              {plan.badge && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-blue-600 text-white text-xs font-bold px-3 py-1 rounded-full">
                  {plan.badge}
                </span>
              )}
              <h3 className="font-bold text-xl text-gray-900">{plan.nombre}</h3>
              <div className="mt-2 mb-4">
                <span className="text-3xl font-extrabold text-gray-900">{plan.precio}</span>
                <span className="text-gray-400 text-sm"> {plan.periodo}</span>
              </div>
              <ul className="space-y-2 mb-6">
                {plan.caracteristicas.map(c => (
                  <li key={c} className="flex items-start gap-2 text-sm text-gray-700">
                    <span className="text-green-500 mt-0.5">✓</span> {c}
                  </li>
                ))}
                {plan.limitaciones.map(l => (
                  <li key={l} className="flex items-start gap-2 text-sm text-gray-400">
                    <span className="mt-0.5">✗</span> {l}
                  </li>
                ))}
              </ul>
              <Link href={plan.href} className={`block text-center text-white font-semibold py-3 rounded-xl transition ${plan.botonColor}`}>
                {plan.precio === '€0' ? 'Empezar gratis' : `Elegir ${plan.nombre}`}
              </Link>
            </div>
          ))}
        </div>
      </section>

      <footer className="border-t bg-white mt-16">
        <div className="max-w-5xl mx-auto px-4 py-8 text-center text-sm text-gray-400">
          <div className="flex items-center justify-center gap-2 mb-2">
            <Shield className="w-4 h-4" />
            <span>Datos seguros y encriptados</span>
          </div>
          <p>© 2025 StockScan. Todos los derechos reservados.</p>
        </div>
      </footer>
    </main>
  )
}
