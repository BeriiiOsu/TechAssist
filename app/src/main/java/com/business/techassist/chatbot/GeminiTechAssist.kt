package com.business.techassist.chatbot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiTechAssist {

    private val client = OkHttpClient()

    private val apiKey = Constants.apiKey
    private val geminiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

    private val conversationHistory = JSONArray()

    init {
        val systemMessage = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", """
                You are TechAssistant, a helpful AI assistant specializing in troubleshooting hardware and software issues 
                related to PCs, smartphones, and laptops only. If someone asks a question not related to tech troubleshooting, 
                kindly let them know you're built only for that purpose. You speak Tagalog and English only.
            """.trimIndent())))
        }
        conversationHistory.put(systemMessage)
    }

    suspend fun ask(userInput: String): String = withContext(Dispatchers.IO) {
        try {
            conversationHistory.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userInput)))
                }
            )

            val requestBodyJson = JSONObject().apply {
                put("contents", conversationHistory)
            }

            val mediaType = "application/json".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(geminiUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val text = JSONObject(response.body?.string() ?: "")
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                conversationHistory.put(
                    JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().put(JSONObject().put("text", text)))
                    }
                )
                return@withContext text
            } else {
                return@withContext "Something went wrong: ${response.code} - ${response.message}"
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "An error occurred. Please try again."
        }
    }
}
