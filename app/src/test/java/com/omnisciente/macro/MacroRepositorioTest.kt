package com.omnisciente.macro

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * Round-trip de serializacion: toda macro guardada debe leerse identica.
 * Cubre los 9 tipos de paso del conjunto cerrado.
 */
class MacroRepositorioTest {

    private fun repoEnTemporal(): MacroRepositorio {
        val dir = Files.createTempDirectory("macros-test").toFile()
        val context = mockk<Context>()
        every { context.filesDir } returns dir
        return MacroRepositorio(context)
    }

    @Test
    fun `serializa y deserializa todos los tipos de paso`() {
        val repo = repoEnTemporal()
        val macro = Macro(
            id = "id-1",
            nombre = "prueba completa",
            appObjetivo = "com.ejemplo.notas",
            pasos = listOf(
                PasoMacro.Esperar(500),
                PasoMacro.TocarPorTexto("Enviar"),
                PasoMacro.EscribirTexto("hola mundo"),
                PasoMacro.Navegar(1),
                PasoMacro.TocarCoordenada(120f, 480f),
                PasoMacro.Deslizar(PasoMacro.Direccion.IZQUIERDA),
                PasoMacro.AbrirApp("com.ejemplo.musica"),
                PasoMacro.ControlMedios(PasoMacro.AccionMedios.SIGUIENTE),
                PasoMacro.AjustarVolumen(PasoMacro.AccionVolumen.BAJAR)
            )
        )

        repo.guardar(macro)
        val cargadas = repo.cargarTodas()

        assertEquals(listOf(macro), cargadas)
    }

    @Test
    fun `guardar con mismo id reemplaza en vez de duplicar`() {
        val repo = repoEnTemporal()
        val original = Macro("id-1", "v1", "com.app", listOf(PasoMacro.Esperar(100)))
        val editada = original.copy(nombre = "v2")

        repo.guardar(original)
        repo.guardar(editada)
        val cargadas = repo.cargarTodas()

        assertEquals(1, cargadas.size)
        assertEquals("v2", cargadas.first().nombre)
    }

    @Test
    fun `eliminar quita solo la macro indicada`() {
        val repo = repoEnTemporal()
        repo.guardar(Macro("a", "una", "com.app", emptyList()))
        repo.guardar(Macro("b", "otra", "com.app", emptyList()))

        repo.eliminar("a")
        val cargadas = repo.cargarTodas()

        assertEquals(1, cargadas.size)
        assertTrue(cargadas.all { it.id == "b" })
    }
}
