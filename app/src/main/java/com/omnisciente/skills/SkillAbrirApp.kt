package com.omnisciente.skills

import android.content.Context
import android.content.Intent
import com.omnisciente.safety.GuardiaContexto

/**
 * Abre apps instaladas por su nombre visible: "abre el navegador".
 * Respeta GuardiaContexto: las apps de banca/pagos no se abren por voz.
 */
class SkillAbrirApp(private val context: Context) : Skill {

    private data class AppInstalada(val etiqueta: String, val paquete: String)

    private val apps: List<AppInstalada> by lazy {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager.queryIntentActivities(launcher, 0).mapNotNull { info ->
            val etiqueta = NumerosEs.normalizar(
                info.loadLabel(context.packageManager).toString()
            ).trim()
            if (etiqueta.isEmpty()) null
            else AppInstalada(etiqueta, info.activityInfo.packageName)
        }.distinctBy { it.paquete }
    }

    private val verbos = listOf("abre ", "abrir ", "lanza ")

    override fun atender(texto: String): String? {
        val t = NumerosEs.normalizar(texto)
        val verbo = verbos.firstOrNull { t.contains(it) } ?: return null

        val nombre = t.substringAfter(verbo).trim()
            .removePrefix("la ").removePrefix("el ").trim()
        if (nombre.isEmpty()) return "Dime que app quieres abrir."

        val app = apps.firstOrNull { it.etiqueta == nombre }
            ?: apps.firstOrNull { it.etiqueta.contains(nombre) }
            ?: apps.firstOrNull { nombre.contains(it.etiqueta) }
            ?: return "No encontre una app llamada $nombre."

        if (!GuardiaContexto.appPermitida(app.paquete)) {
            return "Esa app esta bloqueada para el asistente por seguridad."
        }

        val launch = context.packageManager.getLaunchIntentForPackage(app.paquete)
            ?: return "No pude abrir ${app.etiqueta}."
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(launch) }.fold(
            onSuccess = { "Abriendo ${app.etiqueta}." },
            onFailure = { "No pude abrir ${app.etiqueta}." }
        )
    }

    override fun vocabulario(): List<String> =
        listOf("abre", "abrir", "lanza") + apps.map { it.etiqueta }
}
