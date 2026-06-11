package com.omnisciente.macro

import android.content.res.Resources
import android.media.AudioManager
import android.view.KeyEvent
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
 *  - Un paso AbrirApp revalida GuardiaContexto sobre la app destino y
 *    actualiza la app esperada para el resto de la ejecucion.
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

        if (macro.appObjetivo.isNotEmpty() && !GuardiaContexto.appPermitida(macro.appObjetivo)) {
            return abortar("Esta macro apunta a una app no permitida para automatizacion")
        }

        svc.gestosCancelados = false
        var appEsperada = macro.appObjetivo

        for ((indice, paso) in macro.pasos.withIndex()) {
            if (svc.gestosCancelados) return abortar("Detenida por el usuario")

            val root = svc.rootInActiveWindow
            if (GuardiaContexto.pantallaSensible(root)) {
                return abortar("Pantalla sensible detectada; macro detenida por seguridad")
            }
            val paqueteActual = root?.packageName?.toString()
            if (appEsperada.isNotEmpty() && paqueteActual != null && paqueteActual != appEsperada) {
                return abortar("La app cambio durante la ejecucion")
            }
            if (paqueteActual != null && !GuardiaContexto.appPermitida(paqueteActual)) {
                return abortar("La app activa no esta permitida para automatizacion")
            }

            if (paso is PasoMacro.AbrirApp) {
                if (!GuardiaContexto.appPermitida(paso.paquete)) {
                    return abortar("La app destino no esta permitida para automatizacion")
                }
                appEsperada = paso.paquete
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
            is PasoMacro.TocarCoordenada -> {
                svc.ejecutarToqueAsistido(paso.x, paso.y)
                delay(150)
                true
            }
            is PasoMacro.Deslizar -> {
                val ok = deslizar(svc, paso.direccion)
                if (ok) delay(400) // deja terminar el gesto antes del siguiente paso
                ok
            }
            is PasoMacro.AbrirApp -> {
                val ok = svc.abrirAplicacion(paso.paquete)
                if (ok) delay(1200) // tiempo para que la app aparezca en pantalla
                ok
            }
            is PasoMacro.ControlMedios -> svc.enviarTeclaMedios(
                when (paso.accion) {
                    PasoMacro.AccionMedios.REPRODUCIR_PAUSAR -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    PasoMacro.AccionMedios.SIGUIENTE -> KeyEvent.KEYCODE_MEDIA_NEXT
                    PasoMacro.AccionMedios.ANTERIOR -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                }
            )
            is PasoMacro.AjustarVolumen -> svc.ajustarVolumenMedios(
                when (paso.accion) {
                    PasoMacro.AccionVolumen.SUBIR -> AudioManager.ADJUST_RAISE
                    PasoMacro.AccionVolumen.BAJAR -> AudioManager.ADJUST_LOWER
                    PasoMacro.AccionVolumen.SILENCIAR -> AudioManager.ADJUST_TOGGLE_MUTE
                }
            )
        }

    private fun deslizar(svc: OmniAccessibilityService, direccion: PasoMacro.Direccion): Boolean {
        val dm = Resources.getSystem().displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        return when (direccion) {
            PasoMacro.Direccion.ARRIBA ->
                svc.ejecutarDeslizamientoAsistido(w / 2, h * 0.7f, w / 2, h * 0.3f)
            PasoMacro.Direccion.ABAJO ->
                svc.ejecutarDeslizamientoAsistido(w / 2, h * 0.3f, w / 2, h * 0.7f)
            PasoMacro.Direccion.IZQUIERDA ->
                svc.ejecutarDeslizamientoAsistido(w * 0.8f, h / 2, w * 0.2f, h / 2)
            PasoMacro.Direccion.DERECHA ->
                svc.ejecutarDeslizamientoAsistido(w * 0.2f, h / 2, w * 0.8f, h / 2)
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
