package com.example.dtxvoicerecorder

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecorderData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioRecordDao(): RecorderDao

}