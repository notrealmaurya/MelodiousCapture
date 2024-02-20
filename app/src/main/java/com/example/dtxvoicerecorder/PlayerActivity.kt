package com.example.dtxvoicerecorder

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dtxvoicerecorder.utils.formatDuration
import com.example.dtxvoicerecorder.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var activityPlayerBinding: ActivityPlayerBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var isPlaying: Boolean = false
    private var playBackSpeed= 1.0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityPlayerBinding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(activityPlayerBinding.root)

        val filePath = intent.getStringExtra("filePath")
        var fileName = intent.getStringExtra("fileName")

        activityPlayerBinding.fileNamePlayerActivity.isSelected = true
        activityPlayerBinding.fileNamePlayerActivity.text=fileName

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }



        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            activityPlayerBinding.seekBARPlayerActivity.progress = mediaPlayer.currentPosition
            activityPlayerBinding.durationPLAYEDPlayerActivity.text =
                formatDuration(mediaPlayer.currentPosition.toLong())
            handler.postDelayed(runnable, 0)
        }
        activityPlayerBinding.seekBARPlayerActivity.max = mediaPlayer.duration
        activityPlayerBinding.durationTOTALPlayerActivity.text =
            formatDuration(mediaPlayer.duration.toLong())



        playPauseFunction()


        mediaPlayer.setOnCompletionListener {
            activityPlayerBinding.playPausePlayerActivity.setImageResource(R.drawable.icon_play)
            handler.removeCallbacks(runnable)
        }

        listeners()

    }

    private fun listeners() {


        activityPlayerBinding.PlayerBackBtn.setOnClickListener {
            if (!isPlaying) {
                finish()
            } else {
                finish()
                mediaPlayer.stop()
                mediaPlayer.release()
                handler.removeCallbacks(runnable)
            }
        }

        activityPlayerBinding.playPausePlayerActivity.setOnClickListener {
            playPauseFunction()
        }


        activityPlayerBinding.seekBARPlayerActivity.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }


            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit

        })


        activityPlayerBinding.skipBackwardPlayerActivity.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition - 5000)
            activityPlayerBinding.seekBARPlayerActivity.progress -= 5000
            activityPlayerBinding.durationPLAYEDPlayerActivity.text =
                formatDuration(mediaPlayer.currentPosition.toLong())
            Toast.makeText(this,"5 sec Backward",Toast.LENGTH_SHORT).show()
        }

        activityPlayerBinding.skipForwardPlayerActivity.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition + 5000)
            activityPlayerBinding.seekBARPlayerActivity.progress += 5000
            activityPlayerBinding.durationPLAYEDPlayerActivity.text =
                formatDuration(mediaPlayer.currentPosition.toLong())
            Toast.makeText(this,"5 sec Forward",Toast.LENGTH_SHORT).show()
        }

        activityPlayerBinding.chipPlayerActivity.setOnClickListener{
            if(playBackSpeed != 2.0f){
                playBackSpeed += 0.5f
            }
            else{
                playBackSpeed=0.5f
            }
            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playBackSpeed)
            activityPlayerBinding.chipPlayerActivity.text = "x $playBackSpeed"
        }


    }

    private fun playPauseFunction() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            activityPlayerBinding.playPausePlayerActivity.setImageResource(R.drawable.icon_pause)
            handler.postDelayed(runnable, 0)
            isPlaying = true
        } else {
            mediaPlayer.pause()
            activityPlayerBinding.playPausePlayerActivity.setImageResource(R.drawable.icon_play)
            handler.removeCallbacks(runnable)
            isPlaying = false
        }

    }

    override fun onBackPressed() {
        if (!isPlaying) {
            super.onBackPressed()
        } else {
            super.onBackPressed()
            mediaPlayer.stop()
            mediaPlayer.release()
            handler.removeCallbacks(runnable)
        }
    }


}