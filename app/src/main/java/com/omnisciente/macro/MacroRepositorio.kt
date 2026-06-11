package com.omnisciente.macro

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistencia local de macros en un unico archivo JSON privado.
 * Sin red, sin importacion externa: solo lo que el usuario creo en el editor.
 */
class MacroRepositorio(context: Context) {

    private val archivo = File(context.filesDir, "macros.json")

    fun cargarTodas(): List<Macro> {
        if (!archivo.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(archivo.readText())
            (0 until arr.length()).map { deserializar(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun guardarTodas(macros: List<Macro>) {
        val arr = JSONArray()
        macros.forEach { arr.put(serializar(it)) }
        archivo.writeText(arr.toString())
    }

    fun guardar(macro: Macro) {
        val actuales = cargarTodas().filterNot { it.id == macro.id }
        guardarTodas(actuales + macro)
    }

    fun eliminar(id: String) = guardarTodas(cargarTodas().filterNot { it.id == id })

    private fun serializar(m: Macro): JSONObject {
        val pasos = JSONArray()
        m.pasos.forEach { paso ->
            val obj = JSONObject()
            when (paso) {
                is PasoMacro.Esperar -> obj.put("tipo", "esperar").put("ms", paso.ms)
                is PasoMacro.TocarPorTexto -> obj.put("tipo", "tocar").put("texto", paso.texto)
                is PasoMacro.EscribirTexto -> obj.put("tipo", "escribir").put("contenido", paso.contenido)
                is PasoMacro.Navegar -> obj.put("tipo", "navegar").put("accion", paso.accionGlobal)
            }
            pasos.put(obj)
        }
        return JSONObject()
            .put("id", m.id).put("nombre", m.nombre)
            .put("appObjetivo", m.appObjetivo).put("pasos", pasos)
    }

    private fun deserializar(obj: JSONObject): Macro {
        val pasosArr = obj.getJSONArray("pasos")
        val pasos = (0 until pasosArr.length()).map { i ->
            val p = pasosArr.getJSONObject(i)
            when (p.getString("tipo")) {
                "esperar" -> PasoMacro.Esperar(p.getLong("ms"))
                "tocar" -> PasoMacro.TocarPorTexto(p.getString("texto"))
                "escribir" -> PasoMacro.EscribirTexto(p.getString("contenido"))
                "navegar" -> PasoMacro.Navegar(p.getInt("accion"))
                else -> PasoMacro.Esperar(0)
            }
        }
        return Macro(
            id = obj.getString("id"),
            nombre = obj.getString("nombre"),
            appObjetivo = obj.getString("appObjetivo"),
            pasos = pasos
        )
    }
}
