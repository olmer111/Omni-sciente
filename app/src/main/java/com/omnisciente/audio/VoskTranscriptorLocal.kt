package com.omnisciente.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService

/**
 * Motor de voz offline basado en Vosk. Consume tramas PCM 16-bit / 16 kHz
 * y entrega frases cerradas via [onFrase].
 *
 * Tres modos:
 *  - DORMIDO: solo reacciona a la palabra de activacion ("oye asistente").
 *  - COMANDO: gramatica restringida de alta precision. Si pasa
 *    [timeoutVigiliaMs] sin comandos, vuelve a dormirse solo.
 *  - DICTADO: vocabulario libre para tomar notas.
 */
class VoskTranscriptorLocal(
    context: Context,
    private val sampleRate: Float = 16_000f,
    vocabularioExtra: List<String> = emptyList(),
    private val wakeWordActiva: Boolean = true,
    private val timeoutVigiliaMs: Long = 20_000L,
    private val onFrase: (String) -> Unit,
    private val onListo: () -> Unit = {},
    private val onError: (String) -> Unit = {},
    private val onDespierto: () -> Unit = {},
    private val onDormido: () -> Unit = {}
) : TranscriptorLocal {

    enum class Modo { DORMIDO, COMANDO, DICTADO }
    enum class Salud { CARGANDO, OPERATIVO, FALLO_MODELO }

    @Volatile var salud: Salud = Salud.CARGANDO
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    private var model: Model? = null
    private var recComando: Recognizer? = null
    private var recDictado: Recognizer? = null
    private var recDespertar: Recognizer? = null
    @Volatile private var modo: Modo = if (wakeWordActiva) Modo.DORMIDO else Modo.COMANDO
    @Volatile private var listo = false

    val modoActual: Modo get() = modo

    /** Frases que despiertan al asistente. Palabras comunes del espanol,
     *  presentes con seguridad en el vocabulario del modelo. */
    private val frasesDespertar = listOf("oye asistente", "hola asistente")

    private val gramaticaComandos: String = construirGramatica(vocabularioExtra)
    private val gramaticaDespertar: String =
        JSONArray(frasesDespertar + "[unk]").toString()

    private val dormirRunnable = Runnable {
        if (wakeWordActiva && modo == Modo.COMANDO) {
            cambiarModo(Modo.DORMIDO)
            onDormido()
        }
    }

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
        val arr = JSONArray()
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
                    // Si la gramatica trae palabras fuera del vocabulario del
                    // modelo y el recognizer falla, degradar a decodificacion libre.
                    recComando = runCatching { Recognizer(modelo, sampleRate, gramaticaComandos) }
                        .getOrElse { Recognizer(modelo, sampleRate) }
                    recDictado = Recognizer(modelo, sampleRate)
                    recDespertar = if (wakeWordActiva) {
                        runCatching { Recognizer(modelo, sampleRate, gramaticaDespertar) }
                            .getOrElse { Recognizer(modelo, sampleRate) }
                    } else null
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
            Modo.DORMIDO -> recDespertar?.reset()
        }
        modo = nuevo
        if (nuevo == Modo.COMANDO && wakeWordActiva) reiniciarTemporizadorSueno()
        else mainHandler.removeCallbacks(dormirRunnable)
    }

    /** Despierta al asistente sin decir la frase (p. ej. tocando la burbuja). */
    fun despertar() {
        if (modo != Modo.DORMIDO) return
        cambiarModo(Modo.COMANDO)
        mainHandler.post(onDespierto)
    }

    private fun reiniciarTemporizadorSueno() {
        mainHandler.removeCallbacks(dormirRunnable)
        mainHandler.postDelayed(dormirRunnable, timeoutVigiliaMs)
    }

    override fun alimentar(buffer: ShortArray, length: Int) {
        if (!listo) return
        when (modo) {
            Modo.DORMIDO -> {
                val rec = recDespertar ?: return
                if (rec.acceptWaveForm(buffer, length)) {
                    val texto = extraerCampo(rec.result, "text")
                    if (frasesDespertar.any { texto.contains(it) }) {
                        cambiarModo(Modo.COMANDO)
                        mainHandler.post(onDespierto)
                    }
                }
            }
            Modo.COMANDO, Modo.DICTADO -> {
                val rec = (if (modo == Modo.COMANDO) recComando else recDictado) ?: return
                if (rec.acceptWaveForm(buffer, length)) {
                    val texto = extraerCampo(rec.result, "text")
                    if (texto.isNotBlank() && texto != "[unk]") {
                        if (modo == Modo.COMANDO && wakeWordActiva) reiniciarTemporizadorSueno()
                        mainHandler.post { onFrase(texto) }
                    }
                }
            }
        }
    }

    private fun extraerCampo(json: String, campo: String): String =
        runCatching { JSONObject(json).optString(campo, "").trim() }.getOrDefault("")

    override fun cerrar() {
        listo = false
        mainHandler.removeCallbacks(dormirRunnable)
        recComando?.close(); recComando = null
        recDictado?.close(); recDictado = null
        recDespertar?.close(); recDespertar = null
        model?.close(); model = null
    }
}
