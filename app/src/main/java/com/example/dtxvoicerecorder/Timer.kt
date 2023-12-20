package com.example.dtxvoicerecorder

import android.os.Looper
import android.os.Handler

class Timer(listener: OnTimerTickInterface) {

    interface OnTimerTickInterface {
        fun onTimerTick(duration: String)

    }

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private var duration = 0L
    private var delay = 100L

    init {
        runnable = Runnable {
            duration += delay
            handler.postDelayed(runnable, delay)
            listener.onTimerTick(format())
        }

    }


    fun start() {
        handler.postDelayed(runnable, delay)
    }

    fun pause() {
        handler.removeCallbacks(runnable)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
        duration = 0L
    }

    fun format(): String {
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (duration % (1000 * 60)) / 1000
        val millis = duration % 1000

        val formatted = if (hours > 0) {
            String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, millis / 10)
        } else {
            String.format("%02d:%02d.%02d", minutes, seconds, millis / 10)
        }

        return formatted
    }

}