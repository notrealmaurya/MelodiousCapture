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
    fun insert(vararg audioRecord: RecorderData)

    @Delete
    fun delete(audioRecord: RecorderData)

    @Delete
    fun delete(audioRecord: Array<RecorderData>)

    @Update
    fun update(audioRecord: RecorderData)


}
