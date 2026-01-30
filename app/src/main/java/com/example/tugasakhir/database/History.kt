package com.example.tugasakhir.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class History(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Path gambar hasil prediksi
    val imagePath: String,

    // Label hasil prediksi (Matang, Mentah)
    val label: String,

    // Deskripsi tambahan & rekomendasi
    val description: String,

    // Waktu dibuat (timestamp)
    val createdAt: String
)
