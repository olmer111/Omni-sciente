package com.omnisciente.safety

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fija el contrato de seguridad del GuardiaContexto. Si alguien afloja
 * la lista de bloqueo o invierte "negar por defecto", un test se pone rojo.
 */
class GuardiaContextoTest {

    @Test fun `app de notas comun es permitida`() {
        assertTrue(GuardiaContexto.appPermitida("com.miapp.notas"))
    }

    @Test fun `app de productividad generica es permitida`() {
        assertTrue(GuardiaContexto.appPermitida("com.todoist.android"))
    }

    @Test fun `play store bloqueada por compras`() {
        assertFalse(GuardiaContexto.appPermitida("com.android.vending"))
    }

    @Test fun `servicios gms bloqueados`() {
        assertFalse(GuardiaContexto.appPermitida("com.google.android.gms"))
    }

    @Test fun `paquete con banco en el nombre se niega`() {
        assertFalse(GuardiaContexto.appPermitida("com.mibanco.app"))
        assertFalse(GuardiaContexto.appPermitida("mx.bank.movil"))
    }

    @Test fun `paypal y exchanges cripto se niegan`() {
        assertFalse(GuardiaContexto.appPermitida("com.paypal.android"))
        assertFalse(GuardiaContexto.appPermitida("com.coinbase.android"))
        assertFalse(GuardiaContexto.appPermitida("com.binance.dev"))
    }

    @Test fun `apps de wallet se niegan`() {
        assertFalse(GuardiaContexto.appPermitida("com.algo.wallet"))
    }

    @Test fun `paquete nulo se niega`() {
        assertFalse(GuardiaContexto.appPermitida(null))
    }

    @Test fun `coincidencia es insensible a mayusculas`() {
        assertFalse(GuardiaContexto.appPermitida("COM.MiBanco.APP"))
    }
}
