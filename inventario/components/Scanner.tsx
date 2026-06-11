'use client'

import { useEffect, useRef, useState } from 'react'
import { X, Camera } from 'lucide-react'

interface ScannerProps {
  onDetected: (codigo: string) => void
  onClose: () => void
}

export default function Scanner({ onDetected, onClose }: ScannerProps) {
  const scannerRef = useRef<HTMLDivElement>(null)
  const scannerInstanceRef = useRef<unknown>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let html5QrCode: unknown

    const iniciar = async () => {
      try {
        const { Html5Qrcode } = await import('html5-qrcode')
        html5QrCode = new Html5Qrcode('reader')
        scannerInstanceRef.current = html5QrCode

        await (html5QrCode as { start: (config: unknown, config2: unknown, onSuccess: (code: string) => void, onError: () => void) => Promise<void> }).start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 250 } },
          (codigo: string) => {
            onDetected(codigo)
          },
          () => {}
        )
      } catch {
        setError('No se pudo acceder a la cámara. Verifica los permisos.')
      }
    }

    iniciar()

    return () => {
      if (scannerInstanceRef.current) {
        (scannerInstanceRef.current as { stop: () => Promise<void> }).stop().catch(() => {})
      }
    }
  }, [onDetected])

  return (
    <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl w-full max-w-sm overflow-hidden">
        <div className="flex items-center justify-between p-4 border-b">
          <div className="flex items-center gap-2">
            <Camera className="w-5 h-5 text-blue-600" />
            <span className="font-semibold">Escanear código</span>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-gray-100">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4">
          {error ? (
            <div className="text-center py-8 text-red-500">
              <p>{error}</p>
            </div>
          ) : (
            <div id="reader" ref={scannerRef} className="w-full rounded-lg overflow-hidden" />
          )}
          <p className="text-center text-sm text-gray-500 mt-3">
            Apunta la cámara al código de barras o QR
          </p>
        </div>
      </div>
    </div>
  )
}
