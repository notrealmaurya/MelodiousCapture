package com.example.dtxvoicerecorder.database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(tableName = "audioRecords")
data class RecorderData(
    var fileName: String,
    var filePath: String,
    var timestamp: Long,
    var duration: String,
    var ampsPath: String,
    var fileSize:String

) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @Ignore
    var isChecked = false
}


fun formatDuration(duration: Long): String {
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
            TimeUnit.MINUTES.toSeconds(minutes)

    return String.format("%02d:%02d", minutes, seconds)
}