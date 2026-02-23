package com.example.diplom.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepCounterManager(
    context: Context,
    private val onStepDelta: (Int) -> Unit
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var lastAbsoluteValue: Int? = null

    fun start() {
        val sensor = stepSensor ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val value = event?.values?.firstOrNull()?.toInt() ?: return
        val previous = lastAbsoluteValue
        lastAbsoluteValue = value
        if (previous == null) return

        // TYPE_STEP_COUNTER is cumulative since boot, convert to safe session delta.
        val delta = value - previous
        if (delta in 1..5000) {
            onStepDelta(delta)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
