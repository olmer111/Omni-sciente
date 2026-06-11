package com.omnisciente.skills

/**
 * Conversion de numeros hablados en espanol a enteros. El reconocedor
 * entrega palabras ("treinta y cinco"), no digitos.
 */
object NumerosEs {

    private val palabras = mapOf(
        "cero" to 0, "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3,
        "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8,
        "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13,
        "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17,
        "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20,
        "veintiuno" to 21, "veintidos" to 22, "veintitres" to 23,
        "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26,
        "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
        "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60,
        "setenta" to 70, "ochenta" to 80, "noventa" to 90, "cien" to 100
    )

    private val decenasCompuestas = setOf(30, 40, 50, 60, 70, 80, 90)

    /** Vocabulario de numeros para la gramatica del reconocedor. */
    val vocabulario: List<String> = palabras.keys.toList() + "y"

    /** Numero a partir de un token: digitos ("25") o palabra ("cinco"). */
    fun deToken(token: String): Int? =
        token.toIntOrNull() ?: palabras[normalizar(token)]

    /**
     * Lee un numero simple o compuesto ("treinta y cinco") en [tokens]
     * a partir de [desde]. Devuelve (valor, tokens consumidos) o null.
     */
    fun leer(tokens: List<String>, desde: Int): Pair<Int, Int>? {
        val base = deToken(tokens.getOrNull(desde) ?: return null) ?: return null
        if (base in decenasCompuestas && tokens.getOrNull(desde + 1) == "y") {
            val unidad = deToken(tokens.getOrNull(desde + 2) ?: "")
            if (unidad != null && unidad in 1..9) return (base + unidad) to 3
        }
        return base to 1
    }

    /** Minusculas y sin acentos, para comparar texto reconocido. */
    fun normalizar(s: String): String = s.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u').replace('ñ', 'n')
}
