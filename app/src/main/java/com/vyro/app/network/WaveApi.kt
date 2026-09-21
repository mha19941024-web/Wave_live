package com.vyro.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WaveApi {

    private const val BASE_URL =
        "https://worker-jolly-band-100e.mha19941024.workers.dev"

    private suspend fun request(
        endpoint: String,
        method: String = "GET",
        body: String? = null
    ): String = withContext(Dispatchers.IO) {

        val url = URL(BASE_URL + endpoint)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val responseCode = connection.responseCode

            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response = stream?.bufferedReader()?.use {
                it.readText()
            } ?: ""

            if (responseCode !in 200..299) {
                throw Exception(
                    "Server error $responseCode: $response"
                )
            }

            response

        } finally {
            connection.disconnect()
        }
    }

    suspend fun serverStatus(): String {
        return request("/")
    }

    suspend fun getVideos(): JSONArray {
        val response = request("/videos")
        return try {
            JSONObject(response).optJSONArray("videos")
                ?: JSONArray(response)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    suspend fun getLives(): JSONArray {
        val response = request("/lives")
        return try {
            JSONObject(response).optJSONArray("lives")
                ?: JSONArray(response)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    suspend fun getGifts(): JSONArray {
        val response = request("/gifts")
        return try {
            JSONObject(response).optJSONArray("gifts")
                ?: JSONArray(response)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    suspend fun getUser(userId: String): JSONObject {
        return request("/users/$userId").let {
            JSONObject(it)
        }
    }

    suspend fun createUser(
        username: String,
        displayName: String
    ): JSONObject {

        val body = JSONObject().apply {
            put("username", username)
            put("displayName", displayName)
        }

        return JSONObject(
            request(
                endpoint = "/users",
                method = "POST",
                body = body.toString()
            )
        )
    }

    suspend fun sendGift(
        senderId: String,
        receiverId: String,
        giftId: String,
        quantity: Int = 1
    ): JSONObject {

        val body = JSONObject().apply {
            put("senderId", senderId)
            put("receiverId", receiverId)
            put("giftId", giftId)
            put("quantity", quantity)
        }

        return JSONObject(
            request(
                endpoint = "/gifts/send",
                method = "POST",
                body = body.toString()
            )
        )
    }

    suspend fun addCoins(
        userId: String,
        amount: Int
    ): JSONObject {

        val body = JSONObject().apply {
            put("userId", userId)
            put("amount", amount)
        }

        return JSONObject(
            request(
                endpoint = "/coins/add",
                method = "POST",
                body = body.toString()
            )
        )
    }

    suspend fun createLive(
        userId: String,
        title: String
    ): JSONObject {

        val body = JSONObject().apply {
            put("userId", userId)
            put("title", title)
        }

        return JSONObject(
            request(
                endpoint = "/lives",
                method = "POST",
                body = body.toString()
            )
        )
    }

    suspend fun likeVideo(
        userId: String,
        videoId: String
    ): JSONObject {

        val body = JSONObject().apply {
            put("userId", userId)
            put("videoId", videoId)
        }

        return JSONObject(
            request(
                endpoint = "/videos/$videoId/like",
                method = "POST",
                body = body.toString()
            )
        )
    }
}
