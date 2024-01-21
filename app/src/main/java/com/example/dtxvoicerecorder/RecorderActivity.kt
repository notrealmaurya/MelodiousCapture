package com.example.dtxvoicerecorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.dtxvoicerecorder.database.AppDatabase
import com.example.dtxvoicerecorder.database.RecorderData
import com.example.dtxvoicerecorder.databinding.ActivityRecorderBinding
import com.example.dtxvoicerecorder.utils.isExternalStorageWritable
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RecorderActivity : AppCompatActivity(), Timer.OnTimerTickInterface {

    private lateinit var amplitudes: ArrayList<Float>
    private lateinit var activityRecorderBinding: ActivityRecorderBinding
    private var isRecording: Boolean = false
    private var isPaused: Boolean = false
    private var recorder: MediaRecorder? = null
    private var outputFile: String = ""
    private var dirPath: String = ""
    private lateinit var timer: Timer
    private lateinit var vibrator: Vibrator
    private var duration = ""
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityRecorderBinding = ActivityRecorderBinding.inflate(layoutInflater)
        setContentView(activityRecorderBinding.root)

        amplitudes = ArrayList()
        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        db = Room.databaseBuilder(
            this, AppDatabase::class.java, "audioRecords"
        ).build()

        listeners()


    }

    private fun listeners() {
        activityRecorderBinding.PlayerBackBtn.setOnClickListener {
            finish()
        }

        activityRecorderBinding.recorderActivityPlayPauseRecording.setOnClickListener {
            when {
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        activityRecorderBinding.recorderActivitySaveRecording.setOnClickListener {
            if (isRecording || isPaused) {
                pauseRecording()
                showSaveDialog()
            } else {
                Toast.makeText(this, "Please Record First", Toast.LENGTH_SHORT).show()
            }
        }

        activityRecorderBinding.recorderActivityCancelRecording.setOnClickListener {
            if (isRecording || isPaused) {
                pauseRecording()
                showCancelRecordingDialog()
            }
        }

    }

    private fun showSaveDialog() {
        val bottomSheetDialog =
            BottomSheetDialog(this, R.style.ThemeOverlay_App_BottomSheetDialog)
        val bottomSheetView: View =
            layoutInflater.inflate(R.layout.dialog_bottomsheet_save_recording, null)

        val saveEditText = bottomSheetView.findViewById<EditText>(R.id.save_EditText)
        val saveOKText = bottomSheetView.findViewById<TextView>(R.id.save_OKText)
        val saveCancelText = bottomSheetView.findViewById<TextView>(R.id.save_CancelText)
        bottomSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        saveEditText?.requestFocus()
        saveEditText.selectAll()

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.setCanceledOnTouchOutside(true)

        saveOKText?.setOnClickListener {
            val fileName = saveEditText?.text.toString().trim()
            if (fileName.isNotEmpty()) {
                saveRecording(fileName)
                stopRecording()
                bottomSheetDialog.dismiss()
                Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid file name", Toast.LENGTH_SHORT).show()
            }
        }

        saveCancelText?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    private fun startRecording() {

        if (isExternalStorageWritable()) {
            val downloadsFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dirPath = File(downloadsFolder, "dtxVoicerecorder").absolutePath + "/"
        } else {
            dirPath = "${filesDir.absolutePath}/"
        }

        val currentDate = Date()
        val dateFormat = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.getDefault())
        val dateFormatted = dateFormat.format(currentDate)
        outputFile = "audio_record_$dateFormatted"

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$outputFile.mp3")

            try {
                prepare()
            } catch (e: IOException) {
                return
            }
            start() // Start recording
        }

        isRecording = true
        isPaused = false
        timer.start()
        activityRecorderBinding.recorderActivityPlayPauseRecording.setImageResource(R.drawable.icon_pause_recording)
    }


    private fun saveRecording(fileName: String) {
        val downloadsFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dtxVoicerecorderFolder = File(downloadsFolder, "dtxVoicerecorder")

        if (!dtxVoicerecorderFolder.exists()) {
            dtxVoicerecorderFolder.mkdirs()
        }

        val newFilePath = File(dtxVoicerecorderFolder, "$fileName.mp3")

        if (outputFile != fileName) {
            val existingFilePath = File(dtxVoicerecorderFolder, "$outputFile.mp3")
            existingFilePath.renameTo(newFilePath)
        }


        val fileSizeInBytes = newFilePath.length().toString()
        val fileSize = fileSizeInBytes
        val timeStamp = Date().time
        // var ampsPath = "$newFilePath$fileName"
        val record =
            RecorderData(fileName, newFilePath.absolutePath, timeStamp, duration, "", fileSize)

        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }

        HomeFragment.homeAdapter.notifyDataSetChanged()
    }


    private fun showCancelRecordingDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Cancel Recording")
        alertDialogBuilder.setMessage("Do you want to save or discard the recording?")
        alertDialogBuilder.setPositiveButton("Save") { _, _ ->
            showSaveDialog()
        }
        alertDialogBuilder.setNegativeButton("Discard") { _, _ ->
            stopRecording()
            Toast.makeText(this, "Recording Discarded", Toast.LENGTH_SHORT).show()
        }
        alertDialogBuilder.setNeutralButton("Cancel") { _, _ ->
            // User clicked "Cancel," do nothing
        }
        alertDialogBuilder.create().show()
    }

    private fun resumeRecording() {
        recorder!!.resume()
        isPaused = false
        activityRecorderBinding.recorderActivityPlayPauseRecording.setImageResource(R.drawable.icon_pause_recording)
        timer.start()
    }

    private fun pauseRecording() {
        recorder!!.pause()
        isPaused = true
        activityRecorderBinding.recorderActivityPlayPauseRecording.setImageResource(R.drawable.icon_record)
        timer.pause()
    }


    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        timer.stop()
        isPaused = false
        isRecording = false

        activityRecorderBinding.recorderActivityPlayPauseRecording.setImageResource(R.drawable.icon_record)
        activityRecorderBinding.activityRecorderTimer.text = "00:00:00"
        amplitudes = activityRecorderBinding.waveFormView.clear()
    }

    override fun onTimerTick(duration: String) {
        activityRecorderBinding.activityRecorderTimer.text = duration
        this.duration = duration.dropLast(3)
        activityRecorderBinding.waveFormView.addAmplitude(recorder!!.maxAmplitude.toFloat())

    }


}