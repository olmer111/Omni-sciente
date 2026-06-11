package com.omnisciente.audio

/**
 * Contrato minimo para un motor de voz local (Vosk, etc.).
 */
interface TranscriptorLocal {
    fun alimentar(buffer: ShortArray, length: Int)
    fun cerrar()
}
