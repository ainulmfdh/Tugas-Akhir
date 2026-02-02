package com.example.tugasakhir.database

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import com.example.tugasakhir.model.HistoryModel

class HistoryRepository(context: Context) {

    private val dbHelper = HistorySQLiteHelper(context)

    //  Function Insert data to Database
    fun insertHistory(
        imagePath: String,
        title: String,
        description: String,
        createdAt: String
    ) : Long {

        val values = ContentValues().apply {
            put(HistoryContract.HistoryEntry.COLUMN_IMAGE_PATH, imagePath)
            put(HistoryContract.HistoryEntry.COLUMN_TITLE, title)
            put(HistoryContract.HistoryEntry.COLUMN_DESCRIPTION, description)
            put(HistoryContract.HistoryEntry.COLUMN_CREATED_AT, createdAt)
        }

        val db = dbHelper.writableDatabase
        val result =db.insert(
            HistoryContract.HistoryEntry.TABLE_NAME,
            null,
            values
        )
        db.close()
        return result
    }

    //  Function get All History Data
    fun getAllHistory() : List<HistoryModel> {
        val list = mutableListOf<HistoryModel>()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            HistoryContract.HistoryEntry.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${BaseColumns._ID} DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                list.add(
                    HistoryModel(
                        id = getLong(getColumnIndexOrThrow(BaseColumns._ID)),
                        imagePath = getString(getColumnIndexOrThrow(HistoryContract.HistoryEntry.COLUMN_IMAGE_PATH)),
                        title = getString(getColumnIndexOrThrow(HistoryContract.HistoryEntry.COLUMN_TITLE)),
                        description = getString(getColumnIndexOrThrow(HistoryContract.HistoryEntry.COLUMN_DESCRIPTION)),
                        createdAt = getString(getColumnIndexOrThrow(HistoryContract.HistoryEntry.COLUMN_CREATED_AT))
                    )
                )
            }
        }
        cursor.close()
        db.close()
        return list
    }

    //  DELETE HISTORY BY ID
    fun deleteHistoryById(id: Long): Int{
        val db = dbHelper.writableDatabase
        val result = db.delete(
            HistoryContract.HistoryEntry.TABLE_NAME,
            "${BaseColumns._ID} = ?",
            arrayOf(id.toString())
        )
        db.close()
        return result
    }

    //  Function for delete All History Data
    fun deleteAllHistory(): Int{
        val db = dbHelper.writableDatabase
        val result = db.delete(
            HistoryContract.HistoryEntry.TABLE_NAME,
            null,
            null
        )
        db.close()
        return result
    }
}