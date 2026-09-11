package com.lab.assistant

import android.app.Application
import androidx.room.Room
import com.lab.assistant.data.ChatDatabase

class App : Application() {
    lateinit var db: ChatDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, ChatDatabase::class.java, "lab-chat.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
