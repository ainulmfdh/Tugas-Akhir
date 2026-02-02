package com.example.tugasakhir.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

//  Setup SQLite Helper Database Name & Version
class HistorySQLiteHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        HistoryContract.DATABASE_NAME,
        null,
        HistoryContract.DATABASE_VERSION
    ){

    //  FUNCTION CREATE TABLE
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(HistoryContract.SQL_CREATE_TABLE)
    }

    //  FUNCTION DROP TABLE
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(HistoryContract.SQL_DROP_TABLE)
        onCreate(db)
    }
}