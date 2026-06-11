package com.omnisciente.core

import android.accessibilityservice.AccessibilityService
import android.content.res.Resources
import android.util.DisplayMetrics
import com.omnisciente.audio.VozManager
import com.omnisciente.audio.VoskTranscriptorLocal
import com.omnisciente.service.OmniAccessibilityService
import com.omnisciente.skills.Skill

/**
 * Cerebro local. Recibe texto transcrito y lo mapea a una accion.
 * Procesamiento 100% en dispositivo. Una intencion = un comando explicito.
 * Las skills se consultan en orden tras los comandos del sistema.
 */
class OmniOrchestrator(
    private val voz: VozManager,
    private val cambiarModoVoz: (VoskTranscriptorLocal.Modo) -> Unit = {},
    private val skills: List<Skill> = emptyList()
) {

    sealed class Resultado {
        data class Ejecutado(val accion: String) : Resultado()
        data class Rechazado(val motivo: String) : Resultado()
        object SinCoincidencia : Resultado()
    }

    private val dictado = DictadoController(
        voz = voz,
        obtenerCampo = { OmniAccessibilityService.instance?.obtenerCampoEnfocado() },
        escribir = { nodo, texto, anexar ->
            OmniAccessibilityService.instance?.dictarEnCampo(nodo, texto, anexar) ?: false
        }
    )

    fun procesarComando(textoCrudo: String): Resultado {
        val texto = textoCrudo.trim().lowercase()

        if (dictado.activo) {
            val r = dictado.procesarDictado(textoCrudo)
            if (!dictado.activo) cambiarModoVoz(VoskTranscriptorLocal.Modo.COMANDO)
            return r
        }

        // Comandos que dependen del AccessibilityService. Si no esta activo,
        // se omiten y se deja pasar a las skills (calculadora, alarma, etc.,
        // que no necesitan accesibilidad).
        val servicio = OmniAccessibilityService.instance
        if (servicio != null) {
            val r = procesarComandoAccesibilidad(texto, servicio)
            if (r != null) return r
        } else if (requiereAccesibilidad(texto)) {
            voz.hablar("Para eso necesito el servicio de accesibilidad activo.")
            return Resultado.Rechazado("AccessibilityService no conectado")
        }

        return consultarSkills(texto)
    }

    /** Devuelve el resultado del comando del sistema, o null si no coincide. */
    private fun procesarComandoAccesibilidad(
        texto: String,
        servicio: OmniAccessibilityService
    ): Resultado? = when {
        contiene(texto, "escribir nota", "toma nota", "dictar nota") -> {
            val r = dictado.iniciar()
            if (dictado.activo) cambiarModoVoz(VoskTranscriptorLocal.Modo.DICTADO)
            r
        }
        contiene(texto, "regresa", "atras", "volver") ->
            navegar(servicio, AccessibilityService.GLOBAL_ACTION_BACK, "Regresando")
        contiene(texto, "inicio", "home", "principal") ->
            navegar(servicio, AccessibilityService.GLOBAL_ACTION_HOME, "Yendo al inicio")
        contiene(texto, "recientes", "multitarea") ->
            navegar(servicio, AccessibilityService.GLOBAL_ACTION_RECENTS, "Abriendo recientes")
        contiene(texto, "notificaciones") ->
            navegar(servicio, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Abriendo notificaciones")
        contiene(texto, "toca el centro", "tocar centro", "pulsa centro") ->
            tocarCentro(servicio)
        else -> null
    }

    private fun requiereAccesibilidad(texto: String): Boolean = contiene(
        texto,
        "escribir nota", "toma nota", "dictar nota",
        "regresa", "atras", "volver", "inicio", "home", "principal",
        "recientes", "multitarea", "notificaciones",
        "toca el centro", "tocar centro", "pulsa centro"
    )

    private fun consultarSkills(texto: String): Resultado {
        for (skill in skills) {
            val respuesta = runCatching { skill.atender(texto) }.getOrNull() ?: continue
            voz.hablar(respuesta)
            return Resultado.Ejecutado(respuesta)
        }
        voz.hablar("No reconoci ese comando.")
        return Resultado.SinCoincidencia
    }

    /** Fuerza el regreso a modo comando, cancelando dictado si esta activo. */
    fun forzarReposo() {
        if (dictado.activo) {
            dictado.abortar()
            cambiarModoVoz(VoskTranscriptorLocal.Modo.COMANDO)
        }
    }

    private fun navegar(
        servicio: OmniAccessibilityService,
        codigo: Int,
        confirmacion: String
    ): Resultado {
        val ok = servicio.ejecutarAccionNavegacion(codigo)
        return if (ok) {
            voz.hablar(confirmacion)
            Resultado.Ejecutado(confirmacion)
        } else {
            voz.hablar("No pude completar la accion.")
            Resultado.Rechazado("performGlobalAction devolvio false")
        }
    }

    private fun tocarCentro(servicio: OmniAccessibilityService): Resultado {
        val dm: DisplayMetrics = Resources.getSystem().displayMetrics
        servicio.ejecutarToqueAsistido(dm.widthPixels / 2f, dm.heightPixels / 2f)
        voz.hablar("Tocando el centro.")
        return Resultado.Ejecutado("toque centro")
    }

    private fun contiene(texto: String, vararg claves: String): Boolean =
        claves.any { texto.contains(it) }
}
