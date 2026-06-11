package com.omnisciente.skills

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Fecha y hora actuales, sin red: "que hora es", "que dia es hoy". */
class SkillFechaHora : Skill {

    override fun atender(texto: String): String? {
        val t = NumerosEs.normalizar(texto)
        return when {
            t.contains("que hora") -> {
                val ahora = Calendar.getInstance()
                val h = ahora.get(Calendar.HOUR_OF_DAY)
                val m = ahora.get(Calendar.MINUTE)
                if (m == 0) "Son las $h en punto"
                else "Son las $h y $m minutos"
            }
            t.contains("que dia") || t.contains("que fecha") -> {
                val formato = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
                "Hoy es ${formato.format(Date())}"
            }
            else -> null
        }
    }

    override fun vocabulario(): List<String> =
        listOf("que hora es", "que dia es hoy", "que fecha es hoy")
}
