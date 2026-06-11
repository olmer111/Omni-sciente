package com.omnisciente.safety

import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fija la regla: cualquier campo de contrasena => pantalla sensible;
 * arbol nulo => sensible (negar por defecto). Nunca se lee el contenido.
 */
class PantallaSensibleTest {

    private fun nodo(
        password: Boolean = false,
        hijos: List<AccessibilityNodeInfo> = emptyList()
    ): AccessibilityNodeInfo = mockk(relaxed = true) {
        every { isPassword } returns password
        every { childCount } returns hijos.size
        hijos.forEachIndexed { i, h -> every { getChild(i) } returns h }
    }

    @Test fun `arbol nulo se considera sensible`() {
        assertTrue(GuardiaContexto.pantallaSensible(null))
    }

    @Test fun `pantalla sin campos de password no es sensible`() {
        val raiz = nodo(hijos = listOf(nodo(), nodo()))
        assertFalse(GuardiaContexto.pantallaSensible(raiz))
    }

    @Test fun `campo de password en la raiz marca sensible`() {
        assertTrue(GuardiaContexto.pantallaSensible(nodo(password = true)))
    }

    @Test fun `campo de password anidado marca sensible`() {
        val raiz = nodo(hijos = listOf(nodo(), nodo(hijos = listOf(nodo(password = true)))))
        assertTrue(GuardiaContexto.pantallaSensible(raiz))
    }
}
