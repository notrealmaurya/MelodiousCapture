package com.example.dtxvoicerecorder


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.roundToInt

class WaveFormView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val paint: Paint = Paint()
    private val amplitude = ArrayList<Float>()
    private val spikes = ArrayList<RectF>()

    private val radius = 6f
    private val w = 6f
    private val d = 6f

    private var sw: Float = 0f
    private var sh: Float = 600f
    private var maxSpikes: Int = 0

    init {
        paint.color = Color.rgb(244, 81, 30)
        sw = resources.displayMetrics.widthPixels.toFloat()

        maxSpikes = (sw / (w + d)).toInt()
    }

    fun clear(): ArrayList<Float> {
        var amps = amplitude.clone() as ArrayList<Float>
        amplitude.clear()
        spikes.clear()
        invalidate()

        return amps
    }

    fun addAmplitude(amp: Float) {
        val norm = min((amp.roundToInt() / 7).toFloat(), 400f)
        amplitude.add(norm)

        updateSpikes()
        invalidate()
    }

    private fun updateSpikes() {
        spikes.clear()
        val amps = amplitude.takeLast(maxSpikes)

        for (i in amps.indices) {
            val left = sw + i * (w + d)
            val top = sh / 2 - amps[i] / 2
            val right = left + w
            val bottom = top + amps[i]
            spikes.add(RectF(left, top, right, bottom))
        }

        // Update the offset for continuous movement from right to left
        sw -= d
        if (sw < -w) {
            sw = 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        spikes.forEach {
            canvas.drawRoundRect(it, radius, radius, paint)
        }
    }
}
