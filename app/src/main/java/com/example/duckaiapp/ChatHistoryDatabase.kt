package com.example.duckaiapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class ChatHistoryDatabase(context: Context) : 
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    data class ChatEntry(val id: Long, val content: String, val timestamp: String)

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_CHATS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHATS")
        onCreate(db)
    }

    fun saveChat(content: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CONTENT, content)
            put(COLUMN_TIMESTAMP, getTimestamp())
        }
        db.insert(TABLE_CHATS, null, values)
        db.close()
    }

    fun getAllChats(): List<ChatEntry> {
        val chatList = mutableListOf<ChatEntry>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_CHATS ORDER BY $COLUMN_TIMESTAMP DESC"
        val cursor = db.rawQuery(query, null)
        
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                chatList.add(ChatEntry(id, content, timestamp))
            } while (cursor.moveToNext())
        }
        
        cursor.close()
        db.close()
        return chatList
    }

    fun clearAllHistory() {
        val db = writableDatabase
        db.delete(TABLE_CHATS, null, null)
        db.close()
    }

    private fun getTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    companion object {
        private const val DATABASE_NAME = "chat_history.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_CHATS = "chats"
        private const val COLUMN_ID = "id"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }
}
