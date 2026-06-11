package com.omnisciente.safety

import android.os.Handler
import android.os.Looper
import com.omnisciente.audio.VozManager
import com.omnisciente.core.OmniOrchestrator

/**
 * Punto unico de aborto. Cualquier disparador fisico (agitar, boton de
 * volumen) converge aqui para detener gestos, salir del dictado y volver
 * a reposo seguro. Idempotente y con anti-rebote.
 */
class ParadaEmergencia(
    private val voz: VozManager,
    private val orquestador: OmniOrchestrator,
    private val onAbortar: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var ultimoAborto = 0L
    private val ventanaAntiReboteMs = 800L

    fun abortar(origen: String) {
        val ahora = System.currentTimeMillis()
        if (ahora - ultimoAborto < ventanaAntiReboteMs) return
        ultimoAborto = ahora

        mainHandler.post {
            onAbortar()
            orquestador.forzarReposo()
            voz.hablar("Detenido.")
        }
    }
}
