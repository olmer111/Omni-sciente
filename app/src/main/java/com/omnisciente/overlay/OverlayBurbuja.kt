package com.omnisciente.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Burbuja flotante de estado, arrastrable, dibujada sobre otras apps.
 * Solo indicador + control: no lee ni cubre contenido de otras apps.
 */
class OverlayBurbuja(private val context: Context) {

    enum class Estado(val etiqueta: String, val color: Int) {
        INACTIVO("\u25CF", Color.parseColor("#9E9E9E")),
        ESCUCHANDO("\u25CF", Color.parseColor("#4CAF50")),
        DICTANDO("\u270E", Color.parseColor("#2196F3")),
        SIN_VOZ("\u26A0", Color.parseColor("#F44336"))
    }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var vista: View? = null
    private var indicador: TextView? = null
    private val params = crearParams()

    var onTap: (() -> Unit)? = null

    private fun crearParams(): WindowManager.LayoutParams {
        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipo,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 240
        }
    }

    fun mostrar() {
        if (vista != null) return
        if (!Settings.canDrawOverlays(context)) return

        val contenedor = FrameLayout(context).apply {
            val tv = TextView(context).apply {
                text = Estado.INACTIVO.etiqueta
                setTextColor(Estado.INACTIVO.color)
                textSize = 26f
                setPadding(28, 20, 28, 20)
                setBackgroundColor(Color.parseColor("#22000000"))
            }
            indicador = tv
            addView(tv)
            engancharArrastre(this)
        }
        vista = contenedor
        wm.addView(contenedor, params)
    }

    fun actualizarEstado(estado: Estado) {
        indicador?.post {
            indicador?.text = estado.etiqueta
            indicador?.setTextColor(estado.color)
        }
    }

    private fun engancharArrastre(v: View) {
        var startX = 0; var startY = 0
        var touchX = 0f; var touchY = 0f
        var movido = false

        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = e.rawX; touchY = e.rawY
                    movido = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) movido = true
                    params.x = startX + dx
                    params.y = startY + dy
                    vista?.let { wm.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!movido) onTap?.invoke()
                    true
                }
                else -> false
            }
        }
    }

    fun ocultar() {
        vista?.let { runCatching { wm.removeView(it) } }
        vista = null
        indicador = null
    }
}
