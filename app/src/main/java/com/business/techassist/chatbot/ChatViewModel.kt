package com.business.techassist.chatbot

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    val messageList by lazy {
        mutableStateListOf<MessageModel>()
    }

    val generativeModel : GenerativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = Constants.apiKey
    )

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun sendMessage(question: String) {
        viewModelScope.launch {
            try {
                if (!isValidQuestion(question)) {
                    messageList.add(MessageModel("I can only assist with troubleshooting devices like mobiles, PCs, and gadgets. / Makakatulong lang ako sa pag-troubleshoot ng mga device tulad ng mga mobile, PC, at gadget.", "model"))
                    return@launch
                }

                val chat = generativeModel.startChat(
                    history = messageList.map {
                        content(it.role) { text(it.message) }
                    }
                )

                messageList.add(MessageModel(question, "user"))
                messageList.add(MessageModel("Typing...", "model"))

                val response = chat.sendMessage(question)
                if (messageList.isNotEmpty()) {
                    messageList.removeAt(messageList.size - 1)
                }
                val responseText = response.text ?: "No response from model."
                messageList.add(MessageModel(responseText, "model"))

            } catch (e: Exception) {
                if (messageList.isNotEmpty()) {
                    messageList.removeAt(messageList.size - 1)
                }
                messageList.add(MessageModel("Error: ${e.message}", "model"))
                Log.e("ChatViewModel", "Error: ${e.message}", e)
            }
        }
    }

    private fun isValidQuestion(question: String): Boolean {
        val keywords = listOf(
            // English keywords
            "fix", "problem", "error", "issue", "troubleshoot", "repair", "device", "phone", "PC", "computer", "gadget",
            "crash", "not working", "screen", "battery", "charging", "wifi", "bluetooth", "software", "hardware", "update", "restart", "shutdown", "slow", "lag", "malfunction", "connectivity", "reset", "factory reset", "virus", "app issue", "camera issue", "sound problem", "no signal", "network issue",

            // Tagalog keywords
            "ayos", "problema", "kamalian", "isyu", "pag-aayos", "pag-troubleshoot", "sira", "telepono", "kompyuter", "gadyet", "crash", "hindi gumagana", "screen", "baterya", "pagcha-charge", "wifi", "bluetooth", "software", "hardware", "update", "i-restart", "shutdown", "mabagal", "lag", "sirang bahagi", "konektibidad", "i-reset", "factory reset", "virus", "problema sa app", "problema sa camera", "problema sa tunog", "walang signal", "problema sa network"
        )
        return keywords.any { question.contains(it, ignoreCase = true) }
    }
}
