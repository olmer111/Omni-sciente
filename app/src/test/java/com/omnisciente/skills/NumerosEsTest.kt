package com.omnisciente.skills

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumerosEsTest {

    @Test
    fun `convierte palabras simples`() {
        assertEquals(0, NumerosEs.deToken("cero"))
        assertEquals(7, NumerosEs.deToken("siete"))
        assertEquals(15, NumerosEs.deToken("quince"))
        assertEquals(21, NumerosEs.deToken("veintiuno"))
        assertEquals(100, NumerosEs.deToken("cien"))
    }

    @Test
    fun `acepta digitos literales`() {
        assertEquals(25, NumerosEs.deToken("25"))
    }

    @Test
    fun `acepta acentos`() {
        assertEquals(16, NumerosEs.deToken("dieciséis"))
    }

    @Test
    fun `rechaza palabras que no son numeros`() {
        assertNull(NumerosEs.deToken("hola"))
    }

    @Test
    fun `lee numeros compuestos con y`() {
        val tokens = "treinta y cinco minutos".split(" ")
        assertEquals(35 to 3, NumerosEs.leer(tokens, 0))
    }

    @Test
    fun `numero simple consume un solo token`() {
        val tokens = "siete y media".split(" ")
        assertEquals(7 to 1, NumerosEs.leer(tokens, 0))
    }

    @Test
    fun `decena sin unidad valida no compone`() {
        val tokens = "treinta y media".split(" ")
        assertEquals(30 to 1, NumerosEs.leer(tokens, 0))
    }
}
