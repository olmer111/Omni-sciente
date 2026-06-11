package com.omnisciente.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import com.omnisciente.service.OmniAccessibilityService

/** Comprueba y abre las pantallas de sistema para cada permiso. */
object PermisosHelper {

    fun accesibilidadActiva(context: Context): Boolean {
        val esperado = "${context.packageName}/${OmniAccessibilityService::class.java.name}"
        val activos = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(activos) }
        return splitter.any { it.equals(esperado, ignoreCase = true) }
    }

    fun overlayActivo(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun abrirAjustesAccesibilidad(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun abrirAjustesOverlay(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun abrirExclusionBateria(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
