package com.omnisciente.skills

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

/**
 * Temporizadores via la app de reloj del sistema (intent AlarmClock):
 * "pon un temporizador de cinco minutos".
 */
class SkillTemporizador(private val context: Context) : Skill {

    override fun atender(texto: String): String? {
        val t = NumerosEs.normalizar(texto)
        if (!t.contains("temporizador") && !t.contains("cuenta atras")) return null

        val tokens = t.split(' ').filter { it.isNotBlank() }
        var valor: Int? = null
        var despues = 0
        for (i in tokens.indices) {
            val n = NumerosEs.leer(tokens, i) ?: continue
            valor = n.first
            despues = i + n.second
            break
        }
        if (valor == null || valor <= 0) {
            return "Dime de cuantos minutos o segundos quieres el temporizador."
        }

        val unidad = tokens.getOrNull(despues).orEmpty()
        val (segundos, unidadLegible) = when {
            unidad.startsWith("segundo") -> valor to plural(valor, "segundo")
            unidad.startsWith("hora") -> valor * 3600 to plural(valor, "hora")
            else -> valor * 60 to plural(valor, "minuto")
        }

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, segundos)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Omni-sciente")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.fold(
            onSuccess = { "Temporizador de $valor $unidadLegible puesto." },
            onFailure = { "No encontre una app de reloj para el temporizador." }
        )
    }

    private fun plural(n: Int, base: String) = if (n == 1) base else "${base}s"

    override fun vocabulario(): List<String> = listOf(
        "pon un temporizador de", "temporizador", "cuenta atras",
        "minuto", "minutos", "segundo", "segundos", "hora", "horas"
    ) + NumerosEs.vocabulario
}
