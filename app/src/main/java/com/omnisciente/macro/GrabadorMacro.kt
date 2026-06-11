package com.omnisciente.macro

import android.view.accessibility.AccessibilityEvent
import com.omnisciente.safety.GuardiaContexto

/**
 * Convierte eventos de accesibilidad en pasos de macro editables.
 *
 * Graba acciones SEMANTICAS (que boton tocaste, que texto quedo escrito),
 * nunca coordenadas crudas ni contenido de pantallas sensibles:
 *  - Ignora todo evento de apps bloqueadas por GuardiaContexto.
 *  - Ignora campos de contrasena.
 *  - Ignora la propia interfaz de Omni-sciente.
 *
 * Los pasos resultantes son del mismo conjunto cerrado del editor, asi que
 * el usuario puede revisarlos y corregirlos antes de guardar.
 */
class GrabadorMacro(private val paquetePropio: String) {

    @Volatile var activo = true
        private set

    private val pasos = mutableListOf<PasoMacro>()
    private var ultimoEventoMs = 0L
    private var ultimoPaqueteVentana: String? = null
    private var tsUltimoClick = 0L

    /** Pausa minima entre eventos para registrar un paso Esperar. */
    private val umbralEsperaMs = 800L
    private val esperaMaximaMs = 5_000L

    @Synchronized
    fun procesarEvento(event: AccessibilityEvent) {
        if (!activo) return
        val paquete = event.packageName?.toString() ?: return
        if (paquete == paquetePropio) return
        if (!GuardiaContexto.appPermitida(paquete)) return
        if (event.isPassword) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> registrarClick(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> registrarEscritura(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> registrarCambioDeApp(paquete)
            else -> Unit
        }
    }

    /** Detiene la grabacion y devuelve los pasos capturados. */
    @Synchronized
    fun detener(): List<PasoMacro> {
        activo = false
        return pasos.toList()
    }

    @Synchronized
    fun cantidadPasos(): Int = pasos.size

    private fun registrarClick(event: AccessibilityEvent) {
        val texto = textoDeEvento(event) ?: return
        agregarEsperaSiHuboPausa()
        pasos += PasoMacro.TocarPorTexto(texto)
        tsUltimoClick = System.currentTimeMillis()
        ultimoEventoMs = tsUltimoClick
    }

    private fun registrarEscritura(event: AccessibilityEvent) {
        val texto = event.text.joinToString(" ").trim()
        if (texto.isEmpty()) return
        agregarEsperaSiHuboPausa()
        // Cada letra dispara un evento: conservar solo el estado final del campo.
        val ultimo = pasos.lastOrNull()
        if (ultimo is PasoMacro.EscribirTexto) {
            pasos[pasos.size - 1] = PasoMacro.EscribirTexto(texto)
        } else {
            pasos += PasoMacro.EscribirTexto(texto)
        }
        ultimoEventoMs = System.currentTimeMillis()
    }

    private fun registrarCambioDeApp(paquete: String) {
        val anterior = ultimoPaqueteVentana
        ultimoPaqueteVentana = paquete
        // La primera ventana es la app donde ya estaba el usuario: solo es linea base.
        if (anterior == null || anterior == paquete) return

        // Si el ultimo paso fue el toque al icono en el launcher, el paso
        // AbrirApp lo reemplaza: es mas fiable que buscar el icono por texto.
        val ahora = System.currentTimeMillis()
        val ultimo = pasos.lastOrNull()
        if (ultimo is PasoMacro.TocarPorTexto && ahora - tsUltimoClick < 3_000L) {
            pasos[pasos.size - 1] = PasoMacro.AbrirApp(paquete)
        } else {
            pasos += PasoMacro.AbrirApp(paquete)
        }
        ultimoEventoMs = ahora
    }

    private fun agregarEsperaSiHuboPausa() {
        if (pasos.isEmpty() || ultimoEventoMs == 0L) return
        val pausa = System.currentTimeMillis() - ultimoEventoMs
        if (pausa >= umbralEsperaMs) {
            val ms = (pausa.coerceAtMost(esperaMaximaMs) / 100) * 100
            pasos += PasoMacro.Esperar(ms)
        }
    }

    private fun textoDeEvento(event: AccessibilityEvent): String? {
        val deEvento = event.text.joinToString(" ").trim()
        if (deEvento.isNotEmpty()) return deEvento
        val desc = event.contentDescription?.toString()?.trim()
        return desc?.takeIf { it.isNotEmpty() }
    }
}
