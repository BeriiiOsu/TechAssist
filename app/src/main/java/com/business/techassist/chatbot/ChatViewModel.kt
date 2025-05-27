package com.business.techassist.chatbot

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    val messageList = mutableStateListOf<MessageModel>()
    var isBotTyping by mutableStateOf(false)
        private set

    private val geminiTechAssist = GeminiTechAssist()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun sendMessage(question: String) {
        viewModelScope.launch {
            try {
                isBotTyping = true
                messageList.add(MessageModel(question, "user"))
                messageList.add(MessageModel("Typing...", "model"))

                val responseText = geminiTechAssist.ask(question)

                if (messageList.isNotEmpty()) {
                    messageList.removeAt(messageList.size - 1)
                }

                messageList.add(MessageModel(responseText, "model"))

            } catch (e: Exception) {
                if (messageList.isNotEmpty()) {
                    messageList.removeAt(messageList.size - 1)
                }
                messageList.add(MessageModel("Error: ${e.message}", "model"))
                Log.e("ChatViewModel", "Error: ${e.message}", e)
            } finally {
                isBotTyping = false
            }
        }
    }
}
