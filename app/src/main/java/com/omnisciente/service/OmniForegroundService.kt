package com.omnisciente.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import com.omnisciente.audio.AudioCommandReceiver
import com.omnisciente.audio.TranscriptorLocal
import com.omnisciente.audio.VozManager
import com.omnisciente.audio.VoskTranscriptorLocal
import com.omnisciente.core.OmniOrchestrator
import com.omnisciente.macro.EjecutorMacro
import com.omnisciente.macro.MacroRepositorio
import com.omnisciente.overlay.OverlayBurbuja
import com.omnisciente.safety.DetectorAgitacion
import com.omnisciente.safety.ParadaEmergencia
import com.omnisciente.skills.Skill
import com.omnisciente.skills.SkillAbrirApp
import com.omnisciente.skills.SkillAlarma
import com.omnisciente.skills.SkillCalculadora
import com.omnisciente.skills.SkillEjecutarMacro
import com.omnisciente.skills.SkillFechaHora
import com.omnisciente.skills.SkillTemporizador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class OmniForegroundService : Service(), AudioCommandReceiver.PcmListener {

    companion object {
        private const val CHANNEL_ID = "omni_voice_channel"
        private const val NOTIF_ID = 1001
    }

    private lateinit var voz: VozManager
    private lateinit var orquestador: OmniOrchestrator
    private lateinit var audio: AudioCommandReceiver
    private var transcriptor: TranscriptorLocal? = null
    private var burbuja: OverlayBurbuja? = null
    private var detectorAgitacion: DetectorAgitacion? = null
    private var parada: ParadaEmergencia? = null
    private var avisoFalloEmitido = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        voz = VozManager(this)

        burbuja = OverlayBurbuja(this).apply {
            onTap = { anunciarEstadoActual() }
            mostrar()
        }

        val skills = construirSkills()

        var transcriptorRef: VoskTranscriptorLocal? = null
        orquestador = OmniOrchestrator(
            voz = voz,
            cambiarModoVoz = { modo ->
                transcriptorRef?.cambiarModo(modo)
                burbuja?.actualizarEstado(
                    when (modo) {
                        VoskTranscriptorLocal.Modo.DICTADO -> OverlayBurbuja.Estado.DICTANDO
                        VoskTranscriptorLocal.Modo.COMANDO -> OverlayBurbuja.Estado.ESCUCHANDO
                        VoskTranscriptorLocal.Modo.DORMIDO -> OverlayBurbuja.Estado.DORMIDO
                    }
                )
            },
            skills = skills
        )
        audio = AudioCommandReceiver(context = this, listener = this)

        val vosk = VoskTranscriptorLocal(
            context = this,
            vocabularioExtra = skills.flatMap { it.vocabulario() },
            onFrase = { frase -> onComandoTranscrito(frase) },
            onListo = {
                voz.hablar("Asistente listo. Di: oye asistente, para activarme.")
                burbuja?.actualizarEstado(OverlayBurbuja.Estado.DORMIDO)
            },
            onError = { mensaje -> manejarFalloVoz(mensaje) },
            onDespierto = {
                voz.hablar("Te escucho.")
                burbuja?.actualizarEstado(OverlayBurbuja.Estado.ESCUCHANDO)
            },
            onDormido = {
                burbuja?.actualizarEstado(OverlayBurbuja.Estado.DORMIDO)
            }
        )
        transcriptorRef = vosk
        transcriptor = vosk

        configurarParadaEmergencia()
    }

    /**
     * Skills integradas, todas de procesamiento local. El vocabulario que
     * declaran (incluidos nombres de apps y de macros) se incorpora a la
     * gramatica del reconocedor al arrancar el servicio.
     */
    private fun construirSkills(): List<Skill> {
        val repoMacros = MacroRepositorio(this)
        val ejecutor = EjecutorMacro(voz) { OmniAccessibilityService.instance }
        return listOf(
            SkillEjecutarMacro(repoMacros, ejecutor, scope),
            SkillTemporizador(this),
            SkillAlarma(this),
            SkillCalculadora(),
            SkillFechaHora(),
            SkillAbrirApp(this)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificacion = construirNotificacion()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notificacion)
        }
        audio.iniciar()
        return START_STICKY
    }

    private fun configurarParadaEmergencia() {
        val p = ParadaEmergencia(
            voz = voz,
            orquestador = orquestador,
            onAbortar = { OmniAccessibilityService.instance?.cancelarGestosEnCurso() }
        )
        parada = p
        detectorAgitacion = DetectorAgitacion(this) { p.abortar("agitacion") }.apply { iniciar() }
        OmniAccessibilityService.instance?.onFrenoHardware = { p.abortar("boton_volumen") }
    }

    override fun onAudioFrame(buffer: ShortArray, length: Int) {
        if (Process.getThreadPriority(Process.myTid()) != Process.THREAD_PRIORITY_URGENT_AUDIO) {
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }
        }
        transcriptor?.alimentar(buffer, length)
    }

    override fun onError(message: String) { /* loggea si lo deseas */ }

    fun onComandoTranscrito(texto: String) {
        orquestador.procesarComando(texto)
    }

    private fun manejarFalloVoz(mensaje: String) {
        burbuja?.actualizarEstado(OverlayBurbuja.Estado.SIN_VOZ)
        if (!avisoFalloEmitido) {
            avisoFalloEmitido = true
            voz.hablar(
                "No pude cargar el reconocimiento de voz. Revisa que el modelo " +
                    "este instalado. El asistente sigue activo, pero sin comandos por voz."
            )
        }
        android.util.Log.e("OmniVoz", mensaje)
    }

    private fun anunciarEstadoActual() {
        val t = transcriptor as? VoskTranscriptorLocal
        when (t?.salud) {
            VoskTranscriptorLocal.Salud.OPERATIVO ->
                if (t.modoActual == VoskTranscriptorLocal.Modo.DORMIDO) {
                    // Tocar la burbuja tambien despierta al asistente.
                    t.despertar()
                } else {
                    voz.hablar("Asistente activo. Dime un comando.")
                }
            VoskTranscriptorLocal.Salud.CARGANDO -> voz.hablar("Cargando el reconocimiento de voz, un momento.")
            VoskTranscriptorLocal.Salud.FALLO_MODELO -> voz.hablar("El reconocimiento de voz no esta disponible.")
            null -> voz.hablar("Asistente iniciando.")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        audio.detener()
        voz.apagar()
        transcriptor?.cerrar()
        detectorAgitacion?.detener()
        burbuja?.ocultar()
        super.onDestroy()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID, "Asistente de voz Omni", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Escucha activa del asistente" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Omni-sciente activo")
            .setContentText("Escuchando comandos de voz")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}
