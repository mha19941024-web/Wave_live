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
    sessionToken = token?.trim()?.takeIf { it.isNotEmpty() }
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

    val url = URL(BASE_URL + endpoint)

    val connection =
        url.openConnection() as HttpURLConnection

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
            val token = sessionToken

            if (!token.isNullOrBlank()) {
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
    return request(
        endpoint = "/health"
    )
}

suspend fun createSession(): JSONObject {

    val response =
        request(
            endpoint = "/api/session",
            method = "POST"
        )

    val json =
        JSONObject(response)

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

    val json =
        getFeed()

    return json.optJSONArray("items")
        ?: JSONArray()
}

suspend fun createVideo(
    url: String,
    caption: String = "",
    streamId: String? = null
): JSONObject {

    val body =
        JSONObject().apply {

            if (url.isNotBlank()) {
                put("url", url)
            }

            if (!streamId.isNullOrBlank()) {
                put(
                    "streamId",
                    streamId
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

    val json =
        JSONObject(
            request(
                endpoint =
                    "/api/videos/$videoId/comments?limit=$limit",
                authenticated = true
            )
        )

    return json.optJSONArray("items")
        ?: JSONArray()
}

suspend fun addComment(
    videoId: String,
    text: String
): JSONObject {

    val body =
        JSONObject().apply {
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

    val body =
        JSONObject()

    if (displayName != null) {
        body.put(
            "displayName",
            displayName
        )
    }

    if (bio != null) {
        body.put(
            "bio",
            bio
        )
    }

    if (avatarUrl != null) {
        body.put(
            "avatarUrl",
            avatarUrl
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

suspend fun getLives(): JSONArray {

    val json =
        JSONObject(
            request(
                endpoint = "/api/lives",
                authenticated = true
            )
        )

    return json.optJSONArray("items")
        ?: json.optJSONArray("lives")
        ?: JSONArray()
}

suspend fun createLive(
    title: String
): JSONObject {

    val body =
        JSONObject().apply {
            put(
                "title",
                title.trim()
            )
        }

    return JSONObject(
        request(
            endpoint = "/api/lives",
            method = "POST",
            body = body.toString(),
            authenticated = true
        )
    )
}

suspend fun getGifts(): JSONArray {

    val json =
        JSONObject(
            request(
                endpoint = "/api/gifts",
                authenticated = true
            )
        )

    return json.optJSONArray("items")
        ?: json.optJSONArray("gifts")
        ?: JSONArray()
}

suspend fun sendGift(
    liveId: String,
    receiverId: String,
    giftId: String,
    quantity: Int = 1
): JSONObject {

    val body =
        JSONObject().apply {
            put(
                "liveId",
                liveId
            )

            put(
                "receiverId",
                receiverId
            )

            put(
                "giftId",
                giftId
            )

            put(
                "quantity",
                quantity.coerceAtLeast(1)
            )
        }

    return JSONObject(
        request(
            endpoint = "/api/gifts/send",
            method = "POST",
            body = body.toString(),
            authenticated = true
        )
    )
}

suspend fun getWallet(): JSONObject {
    return getMe().optJSONObject("wallet")
        ?: JSONObject().put(
            "coins",
            0
        )
}

/*
 * لا يتم إضافة العملات من التطبيق مباشرة.
 *
 * الشحن الحقيقي يجب أن يمر من خلال
 * عملية دفع/تحقق في الخادم حتى لا يستطيع
 * المستخدم تعديل رصيده بنفسه.
 */
suspend fun requestCoinRecharge(
    amount: Int
): JSONObject {

    val body =
        JSONObject().apply {
            put(
                "amount",
                amount.coerceAtLeast(1)
            )
        }

    return JSONObject(
        request(
            endpoint = "/api/wallet/recharge",
            method = "POST",
            body = body.toString(),
            authenticated = true
        )
    )
}

}
