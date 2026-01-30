package com.example.tugasakhir.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {

    // Queri untuk memasukkan data ke database
    @Insert
    suspend fun insertHistory(history: History)

    // Queri untuk menampilkan semua data dari database
    @Query("SELECT * FROM history ORDER BY id DESC")
    suspend fun getAllHistory(): List<History>

    // Queri untuk menghapus data dari database
    @Delete
    suspend fun deleteHistory(history: History)

    // Queri untuk menghapus semua data histori
    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()
}
