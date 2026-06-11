package com.omnisciente.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale

/** TTS con ducking: baja el volumen de otras apps mientras Omni habla. */
class VozManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech = TextToSpeech(appContext, this)
    private var focusRequest: AudioFocusRequest? = null

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "ES")
            tts.setAudioAttributes(attrs)
        }
    }

    fun hablar(texto: String) {
        solicitarFoco()
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "omni_utt")
    }

    private fun solicitarFoco() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    fun liberarFoco() {
        focusRequest?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioManager.abandonAudioFocusRequest(it)
        }
    }

    fun apagar() {
        liberarFoco(); tts.stop(); tts.shutdown()
    }
}
