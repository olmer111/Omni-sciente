package com.omnisciente.skills

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillCalculadoraTest {

    private val skill = SkillCalculadora()

    @Test
    fun `suma con palabras`() {
        assertEquals("El resultado es 8", skill.atender("cuanto es cinco mas tres"))
    }

    @Test
    fun `resta puede ser negativa`() {
        assertEquals("El resultado es -2", skill.atender("cuanto es tres menos cinco"))
    }

    @Test
    fun `multiplicacion con numeros compuestos`() {
        assertEquals("El resultado es 70", skill.atender("cuanto es treinta y cinco por dos"))
    }

    @Test
    fun `division exacta`() {
        assertEquals("El resultado es 4", skill.atender("cuanto es doce entre tres"))
    }

    @Test
    fun `division con dividido entre`() {
        assertEquals("El resultado es 5", skill.atender("cuanto es diez dividido entre dos"))
    }

    @Test
    fun `no divide entre cero`() {
        assertEquals("No puedo dividir entre cero", skill.atender("cuanto es diez entre cero"))
    }

    @Test
    fun `ignora comandos ajenos`() {
        assertNull(skill.atender("abre el navegador"))
    }

    @Test
    fun `con disparador pero sin operacion responde ayuda`() {
        val r = skill.atender("cuanto es la vida")
        assertTrue(r != null && r.contains("sumar"))
    }
}
