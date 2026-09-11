package com.lab.assistant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lab.assistant.App
import com.lab.assistant.data.Chat
import com.lab.assistant.data.Message
import com.lab.assistant.data.Prefs
import com.lab.assistant.llm.LlmEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(ctx: Context) : ViewModel() {
    private val app = ctx.applicationContext as App
    private val dao = app.db.dao()
    private val engine = LlmEngine(ctx.applicationContext)
    private val appCtx = ctx.applicationContext

    val chats = dao.chats().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var currentId = MutableStateFlow<String?>(null)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    val streaming = MutableStateFlow("")
    val busy = MutableStateFlow(false)
    val status = MutableStateFlow("")

    fun select(id: String, msgs: List<Message>) {}

    fun newChat(): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        viewModelScope.launch { dao.upsertChat(Chat(id, "New lab chat", now, now)) }
        currentId.value = id
        return id
    }

    fun loadMessages(id: String) {
        viewModelScope.launch {
            dao.messages(id).collect { _messages.value = it }
        }
    }

    fun send(text: String) {
        val id = currentId.value ?: newChat()
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            dao.insertMsg(Message(chatId = id, role = "user", text = text, ts = now))
            dao.touch(id, now, text.take(40))
            busy.value = true; streaming.value = ""
            val path = Prefs.modelPath(appCtx).first()
            val gpu = Prefs.gpu(appCtx).first()
            val temp = Prefs.temp(appCtx).first()
            val maxTok = Prefs.maxTok(appCtx).first()
            val ok = engine.load(path, gpu, maxTok)
            if (!ok && path.isBlank()) {
                dao.insertMsg(Message(chatId = id, role = "assistant", text = "No model loaded. Go to Models tab, download a .task model, then select it. You can still use Labs checklists offline.", ts = System.currentTimeMillis()))
                busy.value = false
                return@launch
            }
            val out = engine.generate(text, temp) { streaming.value = it }
            dao.insertMsg(Message(chatId = id, role = "assistant", text = out, ts = System.currentTimeMillis()))
            streaming.value = ""; busy.value = false
        }
    }

    fun deleteChat(id: String) = viewModelScope.launch {
        dao.clearChat(id); dao.deleteChat(id)
        if (currentId.value == id) currentId.value = null
    }
    fun deleteAll() = viewModelScope.launch {
        dao.deleteAllMsgs(); dao.deleteAllChats(); currentId.value = null
    }
    fun deleteMsg(id: String, uid: Long) = viewModelScope.launch { dao.deleteMsg(id, uid) }
}
