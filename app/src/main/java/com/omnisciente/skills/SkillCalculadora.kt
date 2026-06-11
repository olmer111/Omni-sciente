package com.omnisciente.skills

/** Aritmetica basica por voz: "cuanto es treinta y cinco mas siete". */
class SkillCalculadora : Skill {

    private val operadores = mapOf(
        "mas" to '+', "menos" to '-', "por" to '*',
        "entre" to '/', "dividido" to '/'
    )

    private val disparadores = listOf("cuanto es", "cuanto son", "calcula")

    override fun atender(texto: String): String? {
        val t = NumerosEs.normalizar(texto)
        if (disparadores.none { t.contains(it) }) return null

        val tokens = t.split(' ').filter { it.isNotBlank() }
        var i = 0
        while (i < tokens.size) {
            val a = NumerosEs.leer(tokens, i)
            if (a == null) { i++; continue }

            var j = i + a.second
            val op = operadores[tokens.getOrNull(j)]
            if (op == null) { i++; continue }
            j++
            // "dividido entre/por cinco": saltar la preposicion
            if (tokens[j - 1] == "dividido" &&
                tokens.getOrNull(j) in setOf("entre", "por")
            ) j++

            val b = NumerosEs.leer(tokens, j)
            if (b == null) { i++; continue }

            return calcular(a.first, op, b.first)
        }
        return "Puedo sumar, restar, multiplicar y dividir dos numeros. " +
            "Por ejemplo: cuanto es doce por tres."
    }

    private fun calcular(a: Int, op: Char, b: Int): String = when (op) {
        '+' -> "El resultado es ${a + b}"
        '-' -> "El resultado es ${a - b}"
        '*' -> "El resultado es ${a * b}"
        else ->
            if (b == 0) "No puedo dividir entre cero"
            else {
                val r = a.toDouble() / b
                if (a % b == 0) "El resultado es ${a / b}"
                else "El resultado es ${"%.2f".format(r)}"
            }
    }

    override fun vocabulario(): List<String> =
        disparadores + operadores.keys + NumerosEs.vocabulario
}
