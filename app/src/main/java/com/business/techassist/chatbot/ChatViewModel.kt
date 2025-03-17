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

            "fix", "problem", "error", "issue", "troubleshoot", "repair", "device", "phone", "PC", "computer", "laptop",
            "crash", "not working", "screen", "battery", "charging", "WiFi", "Bluetooth", "software", "hardware", "update",
            "restart", "shutdown", "slow", "lag", "lags", "lagging", "malfunction", "connectivity", "reset", "factory reset",
            "virus", "app issue", "camera issue", "sound problem", "no signal", "network issue", "black screen", "driver issue",
            "blue screen", "BSOD", "disk error", "hard drive failure", "RAM issue", "CPU overheating", "GPU overheating",
            "motherboard failure", "firmware update", "BIOS issue", "touchscreen not working", "charging port issue",
            "power button not working", "overheating", "boot error", "keyboard not working", "mouse issue", "speaker problem",
            "microphone issue", "fan not working", "thermal paste", "broken display", "USB issue", "Ethernet not working",
            "HDMI not working", "mobile data not working", "SIM card issue", "phone overheating", "storage full", "system crash",
            "battery drain", "battery replacement", "app crashing", "screen flickering", "phone freezing", "charging slowly",


            "ayos", "problema", "kamalian", "isyu", "pag-aayos", "pag-troubleshoot", "sira", "telepono", "kompyuter", "laptop",
            "crash", "hindi gumagana", "screen", "baterya", "pagcha-charge", "WiFi", "Bluetooth", "software", "hardware",
            "update", "i-restart", "shutdown", "mabagal", "lag", "nagloko", "sirang bahagi", "konektibidad", "i-reset",
            "factory reset", "virus", "problema sa app", "problema sa camera", "problema sa tunog", "walang signal",
            "problema sa network", "itim ang screen", "problema sa driver", "blue screen", "BSOD", "disk error",
            "hard drive sira", "RAM sira", "CPU sobrang init", "GPU sobrang init", "motherboard sira", "BIOS problema",
            "firmware update", "touchscreen hindi gumagana", "sira ang charging port", "power button sira",
            "sobrang init ng phone", "hindi gumagana ang mikropono", "hindi gumagana ang speaker", "problema sa USB",
            "hindi gumagana ang Ethernet", "hindi gumagana ang HDMI", "hindi gumagana ang mobile data", "problema sa SIM card",
            "storage puno", "sira ang sistema", "malakas ang battery drain", "kailangang palitan ang baterya",
            "nagloko ang app", "nagla-lag ang screen", "nagyeyelo ang phone", "mabagal ang pagcha-charge"
        )
        return keywords.any { question.contains(it, ignoreCase = true) }
    }
}
