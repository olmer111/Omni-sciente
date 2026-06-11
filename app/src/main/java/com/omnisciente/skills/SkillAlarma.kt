package com.omnisciente.skills

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

/**
 * Alarmas via la app de reloj del sistema: "pon una alarma a las siete",
 * "alarma a las ocho y media de la tarde".
 */
class SkillAlarma(private val context: Context) : Skill {

    override fun atender(texto: String): String? {
        val t = NumerosEs.normalizar(texto)
        if (!t.contains("alarma")) return null

        val tokens = t.split(' ').filter { it.isNotBlank() }
        var hora: Int? = null
        var minutos = 0

        for (i in tokens.indices) {
            if (tokens[i] != "las" && tokens[i] != "la") continue
            val n = NumerosEs.leer(tokens, i + 1) ?: continue
            hora = n.first
            val sig = i + 1 + n.second
            if (tokens.getOrNull(sig) == "y") {
                when (tokens.getOrNull(sig + 1)) {
                    "media" -> minutos = 30
                    "cuarto" -> minutos = 15
                    else -> NumerosEs.leer(tokens, sig + 1)?.let { minutos = it.first }
                }
            }
            break
        }

        var h = hora ?: return "Dime a que hora quieres la alarma. Por ejemplo: alarma a las siete y media."
        if (h !in 0..23 || minutos !in 0..59) {
            return "No entendi la hora de la alarma."
        }
        if ((t.contains("tarde") || t.contains("noche")) && h < 12) h += 12

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, h)
            putExtra(AlarmClock.EXTRA_MINUTES, minutos)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Omni-sciente")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.fold(
            onSuccess = {
                if (minutos == 0) "Alarma puesta a las $h."
                else "Alarma puesta a las $h y $minutos."
            },
            onFailure = { "No encontre una app de reloj para la alarma." }
        )
    }

    override fun vocabulario(): List<String> = listOf(
        "pon una alarma a las", "alarma", "a las", "a la",
        "y media", "y cuarto", "de la tarde", "de la noche", "de la manana"
    ) + NumerosEs.vocabulario
}
