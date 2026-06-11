package com.omnisciente.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OmniAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: OmniAccessibilityService? = null
            private set
    }

    /** Callback que el servicio asigna para enrutar el freno de volumen. */
    var onFrenoHardware: (() -> Unit)? = null

    /**
     * Grabador activo, o null. Mientras esta asignado, los eventos de
     * accesibilidad se convierten en pasos de macro; el grabador aplica
     * sus propios filtros de seguridad (GuardiaContexto, contrasenas).
     */
    @Volatile var grabador: com.omnisciente.macro.GrabadorMacro? = null

    @Volatile var gestosCancelados = false

    private var volumenAbajoPresionadoTs = 0L
    private val umbralPulsacionLargaMs = 600L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Sin reaccion automatica: las acciones se disparan bajo demanda
        // desde las macros o comandos de voz. Los eventos solo se observan
        // mientras el usuario tiene una grabacion de macro en curso.
        event?.let { grabador?.procesarEvento(it) }
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (volumenAbajoPresionadoTs == 0L) {
                        volumenAbajoPresionadoTs = System.currentTimeMillis()
                    }
                    if (System.currentTimeMillis() - volumenAbajoPresionadoTs >= umbralPulsacionLargaMs) {
                        volumenAbajoPresionadoTs = 0L
                        onFrenoHardware?.invoke()
                        return true // consume el evento: no baja el volumen
                    }
                }
                KeyEvent.ACTION_UP -> {
                    volumenAbajoPresionadoTs = 0L
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /** Rellena un campo editable con ACTION_SET_TEXT (solo nodos editables). */
    fun completarFormularioAsistido(nodo: AccessibilityNodeInfo, texto: String): Boolean {
        if (!nodo.isEditable) return false
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                texto
            )
        }
        return nodo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Toque programado en (x, y) via dispatchGesture. */
    fun ejecutarToqueAsistido(x: Float, y: Float, duracionMs: Long = 50L) {
        val path = Path().apply { moveTo(x, y) }
        val gesto = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, duracionMs))
            .build()
        dispatchGesture(gesto, null, null)
    }

    /** Deslizamiento entre dos puntos via dispatchGesture. */
    fun ejecutarDeslizamientoAsistido(
        desdeX: Float, desdeY: Float, hastaX: Float, hastaY: Float,
        duracionMs: Long = 300L
    ): Boolean {
        val path = Path().apply { moveTo(desdeX, desdeY); lineTo(hastaX, hastaY) }
        val gesto = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, duracionMs))
            .build()
        return dispatchGesture(gesto, null, null)
    }

    /** Envia una tecla multimedia (play/pausa, siguiente, anterior). */
    fun enviarTeclaMedios(keyCode: Int): Boolean {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return true
    }

    /** Ajusta volumen multimedia (ADJUST_RAISE / ADJUST_LOWER / ADJUST_TOGGLE_MUTE). */
    fun ajustarVolumenMedios(direccion: Int): Boolean {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        return runCatching {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direccion, 0)
        }.isSuccess
    }

    /** Abre una app por nombre de paquete, si esta instalada. */
    fun abrirAplicacion(paquete: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(paquete) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { startActivity(intent) }.isSuccess
    }

    /** Navegacion global del sistema (p. ej. GLOBAL_ACTION_BACK). */
    fun ejecutarAccionNavegacion(codigo: Int): Boolean = performGlobalAction(codigo)

    /** Devuelve el campo de texto con foco de entrada, o null si no es editable. */
    fun obtenerCampoEnfocado(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val foco = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return foco?.takeIf { it.isEditable }
    }

    /**
     * Escribe en un campo editable. Si [anexar] es true, conserva el texto
     * existente y agrega [texto] al final; si no, lo reemplaza.
     */
    fun dictarEnCampo(
        nodo: AccessibilityNodeInfo,
        texto: String,
        anexar: Boolean
    ): Boolean {
        val actual = nodo.text?.toString().orEmpty()
        val nuevoTexto = when {
            !anexar -> texto
            actual.isEmpty() -> texto
            actual.endsWith("\n") -> actual + texto
            else -> "$actual $texto"
        }

        val argsTexto = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                nuevoTexto
            )
        }
        val escrito = nodo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, argsTexto)

        if (escrito) {
            val argsCursor = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, nuevoTexto.length)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, nuevoTexto.length)
            }
            nodo.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, argsCursor)
        }
        return escrito
    }

    /** Cancela cualquier gesto en curso. Lo invoca la parada de emergencia. */
    fun cancelarGestosEnCurso() {
        gestosCancelados = true
    }
}
