package com.example.tugasakhir.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {

    @Insert
    suspend fun insertHistory(history: History)

    @Query("SELECT * FROM history ORDER BY id DESC")
    suspend fun getAllHistory(): List<History>

    @Delete
    suspend fun deleteHistory(history: History)

    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()
}
