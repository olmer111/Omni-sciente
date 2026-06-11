package com.omnisciente.macro

/**
 * Una macro es una lista ordenada de pasos que el USUARIO define en la app.
 * Se serializa a JSON local, pero nunca se ejecuta JSON de fuente externa.
 */
data class Macro(
    val id: String,
    val nombre: String,
    val appObjetivo: String,
    val pasos: List<PasoMacro>
)

/** Acciones permitidas. Conjunto cerrado: no hay "ejecuta codigo arbitrario". */
sealed class PasoMacro {
    data class Esperar(val ms: Long) : PasoMacro()
    data class TocarPorTexto(val texto: String) : PasoMacro()
    data class EscribirTexto(val contenido: String) : PasoMacro()
    data class Navegar(val accionGlobal: Int) : PasoMacro()
    data class TocarCoordenada(val x: Float, val y: Float) : PasoMacro()
    data class Deslizar(val direccion: Direccion) : PasoMacro()
    data class AbrirApp(val paquete: String) : PasoMacro()
    data class ControlMedios(val accion: AccionMedios) : PasoMacro()
    data class AjustarVolumen(val accion: AccionVolumen) : PasoMacro()

    enum class Direccion { ARRIBA, ABAJO, IZQUIERDA, DERECHA }
    enum class AccionMedios { REPRODUCIR_PAUSAR, SIGUIENTE, ANTERIOR }
    enum class AccionVolumen { SUBIR, BAJAR, SILENCIAR }
}
