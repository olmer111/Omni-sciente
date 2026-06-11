package com.omnisciente.core

import android.view.accessibility.AccessibilityNodeInfo
import com.omnisciente.audio.VozManager

/**
 * Maquina de estados para el dictado por voz a un campo de texto.
 * Re-resuelve el campo en cada frase para evitar nodos obsoletos.
 */
class DictadoController(
    private val voz: VozManager,
    private val obtenerCampo: () -> AccessibilityNodeInfo?,
    private val escribir: (AccessibilityNodeInfo, String, Boolean) -> Boolean
) {

    private enum class Estado { INACTIVO, DICTANDO }
    private var estado = Estado.INACTIVO

    val activo: Boolean get() = estado == Estado.DICTANDO

    fun iniciar(): OmniOrchestrator.Resultado {
        if (obtenerCampo() == null) {
            voz.hablar("Toca primero un campo de texto para empezar a dictar.")
            return OmniOrchestrator.Resultado.Rechazado("sin campo enfocado")
        }
        estado = Estado.DICTANDO
        voz.hablar("Listo, dicta tu nota. Di 'termina' para cerrar.")
        return OmniOrchestrator.Resultado.Ejecutado("dictado iniciado")
    }

    fun procesarDictado(texto: String): OmniOrchestrator.Resultado {
        val campo = obtenerCampo()
        if (campo == null) {
            estado = Estado.INACTIVO
            voz.hablar("Perdi el campo de texto. Sali del dictado.")
            return OmniOrchestrator.Resultado.Rechazado("campo perdido")
        }

        return when {
            esControl(texto, "termina", "finaliza", "listo", "cierra nota") -> {
                estado = Estado.INACTIVO
                voz.hablar("Nota cerrada.")
                OmniOrchestrator.Resultado.Ejecutado("dictado finalizado")
            }
            esControl(texto, "cancela dictado", "descarta nota") -> {
                escribir(campo, "", false)
                estado = Estado.INACTIVO
                voz.hablar("Nota descartada.")
                OmniOrchestrator.Resultado.Ejecutado("dictado cancelado")
            }
            esControl(texto, "borra todo", "limpia campo") -> {
                escribir(campo, "", false)
                voz.hablar("Campo vacio.")
                OmniOrchestrator.Resultado.Ejecutado("campo limpiado")
            }
            esControl(texto, "nueva linea", "salto de linea", "punto y aparte") -> {
                escribir(campo, "\n", true)
                OmniOrchestrator.Resultado.Ejecutado("salto de linea")
            }
            else -> {
                val ok = escribir(campo, texto.trim(), true)
                if (ok) OmniOrchestrator.Resultado.Ejecutado("texto anexado")
                else OmniOrchestrator.Resultado.Rechazado("ACTION_SET_TEXT fallo")
            }
        }
    }

    /** Salida inmediata del dictado sin modificar el campo. Para parada de emergencia. */
    fun abortar() {
        estado = Estado.INACTIVO
    }

    private fun esControl(texto: String, vararg claves: String): Boolean {
        val limpio = texto.trim().lowercase()
        return claves.any { limpio == it }
    }
}
