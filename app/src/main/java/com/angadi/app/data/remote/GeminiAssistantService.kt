package com.angadi.app.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiAssistantService {
    fun ask(serverUrl: String, messages: List<Pair<Boolean, String>>, shopContext: String): String {
        require(serverUrl.startsWith("https://") || serverUrl.startsWith("http://")) { "Enter a valid API server URL first." }
        val endpoint = URL(serverUrl.trimEnd('/') + "/api/assistant")
        val body = JSONObject().put("messages", JSONArray().apply {
            messages.takeLast(8).forEach { (fromAssistant, text) ->
                put(JSONObject().put("role", if (fromAssistant) "assistant" else "user").put("text", text.take(700)))
            }
        }).put("context", shopContext.take(3_000)).toString()
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 35_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                val message = runCatching { JSONObject(response).optString("error") }.getOrNull().orEmpty()
                error(message.ifBlank { "Assistant service returned ${connection.responseCode}." })
            }
            JSONObject(response).optString("reply").ifBlank { "I couldn't find a response just now. Please try again." }
        } finally {
            connection.disconnect()
        }
    }

    fun checkServer(serverUrl: String): String {
        require(serverUrl.startsWith("https://") || serverUrl.startsWith("http://")) { "Enter a valid API server URL first." }
        val connection = URL(serverUrl.trimEnd('/') + "/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        return try {
            connection.requestMethod = "GET"
            if (connection.responseCode !in 200..299) error("Server returned ${connection.responseCode}.")
            "API server is connected."
        } finally {
            connection.disconnect()
        }
    }
}
