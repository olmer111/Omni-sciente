package com.omnisciente.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService

/**
 * Motor de voz offline basado en Vosk. Consume tramas PCM 16-bit / 16 kHz
 * y entrega frases cerradas via [onFrase]. Dos recognizers: gramatica
 * restringida para comandos y libre para dictado.
 */
class VoskTranscriptorLocal(
    context: Context,
    private val sampleRate: Float = 16_000f,
    vocabularioExtra: List<String> = emptyList(),
    private val onFrase: (String) -> Unit,
    private val onListo: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) : TranscriptorLocal {

    enum class Modo { COMANDO, DICTADO }
    enum class Salud { CARGANDO, OPERATIVO, FALLO_MODELO }

    @Volatile var salud: Salud = Salud.CARGANDO
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    private var model: Model? = null
    private var recComando: Recognizer? = null
    private var recDictado: Recognizer? = null
    @Volatile private var modo: Modo = Modo.COMANDO
    @Volatile private var listo = false

    private val gramaticaComandos: String = construirGramatica(vocabularioExtra)

    private fun construirGramatica(extra: List<String>): String {
        val frases = linkedSetOf(
            "regresa", "atras", "volver", "inicio", "home", "principal",
            "recientes", "multitarea", "notificaciones",
            "toca el centro", "tocar centro", "pulsa centro",
            "escribir nota", "toma nota", "dictar nota"
        )
        extra.forEach { frase ->
            frase.trim().lowercase().takeIf { it.isNotEmpty() }?.let { frases += it }
        }
        frases += "[unk]"
        val arr = org.json.JSONArray()
        frases.forEach { arr.put(it) }
        return arr.toString()
    }

    init {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        StorageService.unpack(
            context, "model-es", "vosk-model",
            { modelo ->
                runCatching {
                    model = modelo
                    recComando = Recognizer(modelo, sampleRate, gramaticaComandos)
                    recDictado = Recognizer(modelo, sampleRate)
                }.onSuccess {
                    salud = Salud.OPERATIVO
                    listo = true
                    mainHandler.post(onListo)
                }.onFailure { e ->
                    salud = Salud.FALLO_MODELO
                    mainHandler.post { onError("El modelo cargo pero el reconocedor fallo: ${e.message}") }
                }
            },
            { excepcion ->
                salud = Salud.FALLO_MODELO
                mainHandler.post {
                    onError("No se encontro o no se pudo desempaquetar el modelo de voz: ${excepcion.message}")
                }
            }
        )
    }

    fun cambiarModo(nuevo: Modo) {
        if (modo == nuevo) return
        when (modo) {
            Modo.COMANDO -> recComando?.reset()
            Modo.DICTADO -> recDictado?.reset()
        }
        modo = nuevo
    }

    override fun alimentar(buffer: ShortArray, length: Int) {
        if (!listo) return
        val rec = when (modo) {
            Modo.COMANDO -> recComando
            Modo.DICTADO -> recDictado
        } ?: return

        if (rec.acceptWaveForm(buffer, length)) {
            val texto = extraerCampo(rec.result, "text")
            if (texto.isNotBlank() && texto != "[unk]") {
                mainHandler.post { onFrase(texto) }
            }
        }
    }

    private fun extraerCampo(json: String, campo: String): String =
        runCatching { JSONObject(json).optString(campo, "").trim() }.getOrDefault("")

    override fun cerrar() {
        listo = false
        recComando?.close(); recComando = null
        recDictado?.close(); recDictado = null
        model?.close(); model = null
    }
}
