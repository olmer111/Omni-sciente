package com.omnisciente.skills

/**
 * Capacidad de voz integrada. Todas las skills operan 100% en el
 * dispositivo: ninguna hace llamadas de red.
 */
interface Skill {

    /**
     * Intenta atender el comando. Devuelve la respuesta que debe decirse
     * en voz alta, o null si este comando no corresponde a esta skill.
     */
    fun atender(texto: String): String?

    /**
     * Palabras o frases que esta skill entiende, para incluirlas en la
     * gramatica del reconocedor de voz. Vacio = solo vocabulario base.
     */
    fun vocabulario(): List<String> = emptyList()
}
