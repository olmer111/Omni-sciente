package com.omnisciente.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captura PCM en un hilo dedicado usando VOICE_COMMUNICATION
 * (cancelacion de eco + AGC por hardware cuando el dispositivo lo soporta).
 *
 * No transcribe por si mismo: entrega tramas PCM crudas a traves de
 * [PcmListener] para conectar Vosk encima.
 */
class AudioCommandReceiver(
    private val context: Context,
    private val sampleRate: Int = 16_000,
    private val listener: PcmListener
) {

    interface PcmListener {
        fun onAudioFrame(buffer: ShortArray, length: Int)
        fun onError(message: String) {}
    }

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

    private val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
        .coerceAtLeast(sampleRate / 4)

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private val running = AtomicBoolean(false)

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun iniciar() {
        if (running.get()) return
        if (!tienePermiso()) {
            listener.onError("Falta el permiso RECORD_AUDIO")
            return
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            channelConfig,
            audioEncoding,
            minBuffer * 2
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            listener.onError("AudioRecord no se inicializo")
            record.release()
            return
        }

        audioRecord = record
        running.set(true)
        record.startRecording()

        captureThread = Thread({ buclesCaptura(record) }, "OmniAudioCapture").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun buclesCaptura(record: AudioRecord) {
        val frame = ShortArray(minBuffer)
        while (running.get()) {
            val read = record.read(frame, 0, frame.size)
            if (read > 0) {
                listener.onAudioFrame(frame, read)
            } else if (read < 0) {
                listener.onError("Error de lectura PCM: $read")
                break
            }
        }
    }

    fun detener() {
        running.set(false)
        captureThread?.join(500)
        captureThread = null
        audioRecord?.run {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop()
            release()
        }
        audioRecord = null
    }
}
