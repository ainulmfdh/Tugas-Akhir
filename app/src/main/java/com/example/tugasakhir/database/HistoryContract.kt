package com.example.tugasakhir.database

import android.provider.BaseColumns

object HistoryContract {

    //  VARIABLE TABLE HISTORY
    object HistoryEntry : BaseColumns{
        const val TABLE_NAME = "history"
        const val COLUMN_IMAGE_PATH = "image_path"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_CREATED_AT = "created_at"
    }

    //  DATABASE NAME & VERSION
    const val DATABASE_NAME = "history.db"
    const val DATABASE_VERSION = 1

    //  QUERY CREATE TABLE
    const val SQL_CREATE_TABLE = """
        CREATE TABLE ${HistoryEntry.TABLE_NAME} (
            ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${HistoryEntry.COLUMN_IMAGE_PATH} TEXT NOT NULL,
            ${HistoryEntry.COLUMN_TITLE} TEXT NOT NULL,
            ${HistoryEntry.COLUMN_DESCRIPTION} TEXT NOT NULL,
            ${HistoryEntry.COLUMN_CREATED_AT} TEXT NOT NULL
        )
    """

    //  DROP TABLE
    const val SQL_DROP_TABLE =
        "DROP TABLE IF EXISTS ${HistoryEntry.TABLE_NAME}"
}