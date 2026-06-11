package com.omnisciente.safety

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detecta agitacion deliberada via acelerometro. Requiere varios picos
 * sobre el umbral en una ventana corta, para que un golpe casual no aborte.
 */
class DetectorAgitacion(
    context: Context,
    private val umbralG: Float = 2.7f,
    private val sacudonesNecesarios: Int = 3,
    private val ventanaMs: Long = 1000L,
    private val onAgitacion: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val acelerometro: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var conteoSacudones = 0
    private var primerSacudonTs = 0L
    private var ultimoSacudonTs = 0L

    fun iniciar() {
        acelerometro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun detener() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val gFuerza = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

        if (gFuerza > umbralG) {
            val ahora = System.currentTimeMillis()
            if (ahora - ultimoSacudonTs > ventanaMs) {
                conteoSacudones = 0
                primerSacudonTs = ahora
            }
            if (ahora - ultimoSacudonTs > 80) {
                conteoSacudones++
                ultimoSacudonTs = ahora
            }
            if (conteoSacudones >= sacudonesNecesarios && ahora - primerSacudonTs <= ventanaMs) {
                conteoSacudones = 0
                onAgitacion()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
