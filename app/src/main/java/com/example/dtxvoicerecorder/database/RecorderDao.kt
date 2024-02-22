package com.example.dtxvoicerecorder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecorderDao {
    @Query("SELECT * FROM audioRecords")
    fun getAll(): List<RecorderData>

    @Query("SELECT * FROM audioRecords WHERE fileName LIKE :query")
    fun searchDatabase(query: String): List<RecorderData>

    @Insert
    fun insert(vararg audioRecords: RecorderData)

    @Delete
    fun delete(audioRecords: RecorderData)


    @Update
    fun update(audioRecords: RecorderData)


}
