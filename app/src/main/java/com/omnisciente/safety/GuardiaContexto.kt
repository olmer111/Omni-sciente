package com.omnisciente.safety

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Cortafuegos de seguridad. Decide si la automatizacion tiene permitido
 * actuar sobre la pantalla actual. Ante la duda, NIEGA.
 *
 * Filosofia invertida respecto a un troyano: no leemos ni copiamos nada
 * de pantallas sensibles, simplemente nos negamos a tocarlas.
 */
object GuardiaContexto {

    private val paquetesBloqueados = setOf(
        "com.android.vending",
        "com.google.android.gms"
    )

    private val prefijosSensibles = listOf(
        "com.bank", "com.bbva", "com.santander", "com.paypal",
        "com.coinbase", "com.binance", "wallet", "bank", "banco"
    )

    /** Esta permitido automatizar sobre este paquete? */
    fun appPermitida(packageName: String?): Boolean {
        if (packageName == null) return false
        if (packageName in paquetesBloqueados) return false
        val lower = packageName.lowercase()
        return prefijosSensibles.none { lower.contains(it) }
    }

    /**
     * La pantalla actual contiene campos sensibles (contrasena / tarjeta)?
     * Recorre el arbol buscando campos de contrasena SIN leer su contenido.
     */
    fun pantallaSensible(root: AccessibilityNodeInfo?): Boolean {
        root ?: return true
        return contieneCampoProtegido(root)
    }

    private fun contieneCampoProtegido(nodo: AccessibilityNodeInfo): Boolean {
        if (nodo.isPassword) return true
        for (i in 0 until nodo.childCount) {
            val hijo = nodo.getChild(i) ?: continue
            if (contieneCampoProtegido(hijo)) return true
        }
        return false
    }
}
