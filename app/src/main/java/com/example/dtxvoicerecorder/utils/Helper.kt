package com.example.dtxvoicerecorder.utils

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun getFormattedFileSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        "%.1f %s",
        sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}

fun getFormattedDate(lastModified: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.getDefault())
    return sdf.format(Date(lastModified))
}

fun getFileExtension(fileName: String): String {
    val dotIndex = fileName.lastIndexOf('.')
    return if (dotIndex != -1) {
        fileName.substring(dotIndex + 1)
    } else {
        ""
    }
}


fun isFilePresent(filePath: String): Boolean {
    val file = File(filePath)
    return file.exists()
}

 fun isExternalStorageWritable(): Boolean {
    val state = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED == state
}



fun formatDuration(duration: Long): String {
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
            TimeUnit.MINUTES.toSeconds(minutes)

    return String.format("%02d:%02d", minutes, seconds)
}

