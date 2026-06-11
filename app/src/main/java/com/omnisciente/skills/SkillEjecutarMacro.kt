package com.omnisciente.skills

import com.omnisciente.macro.EjecutorMacro
import com.omnisciente.macro.MacroRepositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Lanza por voz las macros que el usuario creo en el editor:
 * "ejecuta enviar reporte". El EjecutorMacro mantiene todas sus
 * garantias de seguridad (GuardiaContexto en cada paso, frenos).
 */
class SkillEjecutarMacro(
    private val repo: MacroRepositorio,
    private val ejecutor: EjecutorMacro,
    private val scope: CoroutineScope
) : Skill {

    private val verbos = listOf("ejecuta ", "ejecutar ", "lanza la macro ", "macro ")

    override fun atender(texto: String): String? {
        val t = NumerosEs.normalizar(texto)
        val verbo = verbos.firstOrNull { t.contains(it) } ?: return null

        val nombre = t.substringAfter(verbo).trim()
            .removePrefix("la macro ").removePrefix("macro ").removePrefix("la ").trim()
        if (nombre.isEmpty()) return null

        val macros = repo.cargarTodas()
        val macro = macros.firstOrNull { NumerosEs.normalizar(it.nombre) == nombre }
            ?: macros.firstOrNull { NumerosEs.normalizar(it.nombre).contains(nombre) }
            ?: return "No encontre una macro llamada $nombre."

        if (macro.pasos.isEmpty()) return "La macro ${macro.nombre} no tiene pasos."

        scope.launch { ejecutor.ejecutar(macro) }
        return "Ejecutando ${macro.nombre}."
    }

    override fun vocabulario(): List<String> =
        listOf("ejecuta", "ejecutar", "macro") +
            repo.cargarTodas().map { NumerosEs.normalizar(it.nombre) }
}
