package com.omnisciente.core

import com.omnisciente.audio.VozManager
import com.omnisciente.skills.Skill
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El orquestador debe poder atender skills puras (sin accesibilidad)
 * cuando el servicio no esta activo, y rechazar solo los comandos que
 * si dependen de el.
 */
class OmniOrchestratorTest {

    private val voz = mockk<VozManager>(relaxed = true)

    private fun skillFija(disparador: String, respuesta: String) = object : Skill {
        override fun atender(texto: String): String? =
            if (texto.contains(disparador)) respuesta else null
    }

    @Test
    fun `enruta a una skill cuando no hay comando de sistema`() {
        val orq = OmniOrchestrator(
            voz = voz,
            skills = listOf(skillFija("calcula", "El resultado es 4"))
        )

        val r = orq.procesarComando("calcula doce entre tres")

        assertTrue(r is OmniOrchestrator.Resultado.Ejecutado)
        verify { voz.hablar("El resultado es 4") }
    }

    @Test
    fun `sin coincidencia avisa que no reconocio`() {
        val orq = OmniOrchestrator(voz = voz, skills = emptyList())

        val r = orq.procesarComando("algo que nadie entiende")

        assertTrue(r is OmniOrchestrator.Resultado.SinCoincidencia)
        verify { voz.hablar("No reconoci ese comando.") }
    }

    @Test
    fun `comando de navegacion sin accesibilidad se rechaza`() {
        val orq = OmniOrchestrator(voz = voz, skills = emptyList())

        // Sin AccessibilityService activo en el entorno de test.
        val r = orq.procesarComando("regresa")

        assertTrue(r is OmniOrchestrator.Resultado.Rechazado)
    }
}
