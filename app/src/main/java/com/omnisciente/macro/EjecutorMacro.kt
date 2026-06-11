package com.omnisciente.macro

import android.view.accessibility.AccessibilityNodeInfo
import com.omnisciente.audio.VozManager
import com.omnisciente.safety.GuardiaContexto
import com.omnisciente.service.OmniAccessibilityService
import kotlinx.coroutines.delay

/**
 * Ejecuta una macro paso a paso, revalidando seguridad en CADA paso.
 *
 * Garantias:
 *  - No arranca si la app activa no coincide con la de la macro.
 *  - Se detiene si aparece una pantalla sensible (contrasena/banca).
 *  - Respeta la parada de emergencia (flag gestosCancelados).
 *  - Sin reintento silencioso: ante fallo, se detiene y avisa.
 */
class EjecutorMacro(
    private val voz: VozManager,
    private val servicio: () -> OmniAccessibilityService?
) {
    sealed class Resultado {
        object Completada : Resultado()
        data class Abortada(val motivo: String) : Resultado()
    }

    suspend fun ejecutar(macro: Macro): Resultado {
        val svc = servicio() ?: return abortar("Servicio de accesibilidad inactivo")

        if (!GuardiaContexto.appPermitida(macro.appObjetivo)) {
            return abortar("Esta macro apunta a una app no permitida para automatizacion")
        }

        svc.gestosCancelados = false

        for ((indice, paso) in macro.pasos.withIndex()) {
            if (svc.gestosCancelados) return abortar("Detenida por el usuario")

            val root = svc.rootInActiveWindow
            if (GuardiaContexto.pantallaSensible(root)) {
                return abortar("Pantalla sensible detectada; macro detenida por seguridad")
            }
            val paqueteActual = root?.packageName?.toString()
            if (paqueteActual != null && paqueteActual != macro.appObjetivo) {
                return abortar("La app cambio durante la ejecucion")
            }

            val ok = ejecutarPaso(svc, paso)
            if (!ok) return abortar("Fallo el paso ${indice + 1}")
        }

        voz.hablar("Macro ${macro.nombre} completada.")
        return Resultado.Completada
    }

    private suspend fun ejecutarPaso(svc: OmniAccessibilityService, paso: PasoMacro): Boolean =
        when (paso) {
            is PasoMacro.Esperar -> { delay(paso.ms); true }
            is PasoMacro.Navegar -> svc.ejecutarAccionNavegacion(paso.accionGlobal)
            is PasoMacro.TocarPorTexto -> {
                val nodo = buscarPorTexto(svc.rootInActiveWindow, paso.texto)
                nodo?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
            }
            is PasoMacro.EscribirTexto -> {
                val campo = svc.obtenerCampoEnfocado()
                if (campo != null) svc.completarFormularioAsistido(campo, paso.contenido) else false
            }
        }

    private fun buscarPorTexto(root: AccessibilityNodeInfo?, texto: String): AccessibilityNodeInfo? {
        root ?: return null
        val coincidencias = root.findAccessibilityNodeInfosByText(texto)
        return coincidencias.firstOrNull { it.isClickable } ?: coincidencias.firstOrNull()
    }

    private fun abortar(motivo: String): Resultado.Abortada {
        voz.hablar("Macro detenida. $motivo.")
        return Resultado.Abortada(motivo)
    }
}
