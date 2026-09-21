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

    private var sessionToken: String? = null

    fun setSessionToken(token: String?) {
        sessionToken = token
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun getSessionToken(): String? {
        return sessionToken
    }

    private suspend fun request(
        endpoint: String,
        method: String = "GET",
        body: String? = null,
        authenticated: Boolean = false
    ): String = withContext(Dispatchers.IO) {

        val connection =
            URL(BASE_URL + endpoint)
                .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            if (authenticated) {
                sessionToken?.let { token ->
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )
                }
            }

            if (body != null) {
                connection.doOutput = true

                connection.outputStream.use { output ->
                    output.write(
                        body.toByteArray(Charsets.UTF_8)
                    )
                }
            }

            val responseCode =
                connection.responseCode

            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response =
                stream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

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
        return request("/health")
    }

    suspend fun createSession(): JSONObject {

        val response = request(
            endpoint = "/api/session",
            method = "POST"
        )

        val json = JSONObject(response)

        val token =
            json.optString("token")

        if (token.isNotBlank()) {
            setSessionToken(token)
        }

        return json
    }

    suspend fun getMe(): JSONObject {
        return JSONObject(
            request(
                endpoint = "/api/me",
                authenticated = true
            )
        )
    }

    suspend fun getFeed(
        limit: Int = 20,
        cursor: Int = 0
    ): JSONObject {

        return JSONObject(
            request(
                endpoint =
                    "/api/feed?limit=$limit&cursor=$cursor",
                authenticated = true
            )
        )
    }

    suspend fun getVideos(): JSONArray {
        return getFeed()
            .optJSONArray("items")
            ?: JSONArray()
    }

    suspend fun createVideo(
        url: String,
        caption: String = "",
        streamId: String? = null
    ): JSONObject {

        val body = JSONObject().apply {

            if (url.isNotBlank()) {
                put("url", url.trim())
            }

            if (!streamId.isNullOrBlank()) {
                put(
                    "streamId",
                    streamId.trim()
                )
            }

            put(
                "caption",
                caption.trim()
            )
        }

        return JSONObject(
            request(
                endpoint = "/api/videos",
                method = "POST",
                body = body.toString(),
                authenticated = true
            )
        )
    }

    suspend fun likeVideo(
        videoId: String
    ): JSONObject {

        return JSONObject(
            request(
                endpoint =
                    "/api/videos/$videoId/like",
                method = "POST",
                authenticated = true
            )
        )
    }

    suspend fun unlikeVideo(
        videoId: String
    ): JSONObject {

        return JSONObject(
            request(
                endpoint =
                    "/api/videos/$videoId/like",
                method = "DELETE",
                authenticated = true
            )
        )
    }

    suspend fun getComments(
        videoId: String,
        limit: Int = 30
    ): JSONArray {

        return JSONObject(
            request(
                endpoint =
                    "/api/videos/$videoId/comments?limit=$limit",
                authenticated = true
            )
        ).optJSONArray("items")
            ?: JSONArray()
    }

    suspend fun addComment(
        videoId: String,
        text: String
    ): JSONObject {

        val body = JSONObject().apply {
            put(
                "text",
                text.trim()
            )
        }

        return JSONObject(
            request(
                endpoint =
                    "/api/videos/$videoId/comments",
                method = "POST",
                body = body.toString(),
                authenticated = true
            )
        )
    }

    suspend fun followUser(
        userIdOrUsername: String
    ): JSONObject {

        return JSONObject(
            request(
                endpoint =
                    "/api/users/$userIdOrUsername/follow",
                method = "POST",
                authenticated = true
            )
        )
    }

    suspend fun unfollowUser(
        userIdOrUsername: String
    ): JSONObject {

        return JSONObject(
            request(
                endpoint =
                    "/api/users/$userIdOrUsername/follow",
                method = "DELETE",
                authenticated = true
            )
        )
    }

    suspend fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        avatarUrl: String? = null
    ): JSONObject {

        val body = JSONObject()

        displayName?.let {
            body.put(
                "displayName",
                it.trim()
            )
        }

        bio?.let {
            body.put(
                "bio",
                it.trim()
            )
        }

        avatarUrl?.let {
            body.put(
                "avatarUrl",
                it.trim()
            )
        }

        return JSONObject(
            request(
                endpoint = "/api/profile",
                method = "PATCH",
                body = body.toString(),
                authenticated = true
            )
        )
    }

    suspend fun getGifts(): JSONArray {

        return JSONObject(
            request(
                endpoint = "/api/gifts"
            )
        ).optJSONArray("items")
            ?: JSONArray()
    }

    /*
     * إنشاء بث مباشر حقيقي
     */
    suspend fun createLive(
        title: String
    ): JSONObject {

        val body = JSONObject().apply {
            put(
                "title",
                title.trim()
            )
        }

        return JSONObject(
            request(
                endpoint = "/api/live/create",
                method = "POST",
                body = body.toString(),
                authenticated = true
            )
        )
    }

    /*
     * جلب بيانات بث مباشر
     */
    suspend fun getLive(
        liveId: String
    ): JSONObject {

        return JSONObject(
            request(
                endpoint = "/api/live/$liveId",
                authenticated = true
            )
        )
    }

    /*
     * تغيير حالة البث
     *
     * status:
     * created
     * live
     * ended
     */
    suspend fun updateLive(
        liveId: String,
        status: String? = null,
        title: String? = null
    ): JSONObject {

        val body = JSONObject()

        status?.let {
            body.put(
                "status",
                it
            )
        }

        title?.let {
            body.put(
                "title",
                it.trim()
            )
        }

        return JSONObject(
            request(
                endpoint = "/api/live/$liveId",
                method = "PATCH",
                body = body.toString(),
                authenticated = true
            )
        )
    }

    /*
     * إرسال هدية داخل البث
     */
    suspend fun sendLiveGift(
        liveId: String,
        giftId: String,
        quantity: Int = 1,
        receiverUserId: String? = null
    ): JSONObject {

        val body = JSONObject().apply {

            put(
                "giftId",
                giftId
            )

            put(
                "quantity",
                quantity.coerceAtLeast(1)
            )

            receiverUserId?.let {
                put(
                    "receiverUserId",
                    it
                )
            }
        }

        return JSONObject(
            request(
                endpoint =
                    "/api/live/$liveId/gifts",
                method = "POST",
                body = body.toString(),
                authenticated = true
            )
        )
    }

    /*
     * رفع فيديو مباشر إلى Cloudflare Stream
     */
    suspend fun createDirectUpload(): JSONObject {

        return JSONObject(
            request(
                endpoint =
                    "/api/upload/direct",
                method = "POST",
                authenticated = true
            )
        )
    }

    /*
     * بيانات المحفظة الحالية
     */
    suspend fun getWallet(): JSONObject {

        return getMe()
            .optJSONObject("wallet")
            ?: JSONObject().apply {
                put("coins", 0)
            }
    }

    /*
     * رصيد العملات فقط
     */
    suspend fun getCoins(): Int {
        return getWallet()
            .optInt("coins", 0)
    }

    /*
     * ملاحظة:
     * لا نضيف العملات محليًا.
     *
     * الشحن الحقيقي يجب أن يتم بعد
     * تأكيد عملية الدفع على الخادم.
     */
}
