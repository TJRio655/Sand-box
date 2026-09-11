package com.lab.assistant.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val chatId: String,
    val role: String,
    val text: String,
    val ts: Long
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun chats(): Flow<List<Chat>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY ts ASC")
    fun messages(chatId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChat(c: Chat)

    @Insert
    suspend fun insertMsg(m: Message)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChat(chatId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId AND uid = :uid")
    suspend fun deleteMsg(chatId: String, uid: Long)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("DELETE FROM messages")
    suspend fun deleteAllMsgs()

    @Query("UPDATE chats SET updatedAt = :ts, title = :title WHERE id = :id")
    suspend fun touch(id: String, ts: Long, title: String)
}

@Database(entities = [Chat::class, Message::class], version = 1, exportSchema = false)
abstract class ChatDatabase : androidx.room.RoomDatabase() {
    abstract fun dao(): ChatDao
}
