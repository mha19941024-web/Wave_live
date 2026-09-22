package com.vyro.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WaveApi {

    private const val BASE_URL =
        "https://worker-jolly-band-100e.mha19941024.workers.dev"

    private const val PREFS_NAME = "vyro_session"
    private const val TOKEN_KEY = "token"

    data class ApiResult(
        val success: Boolean,
        val statusCode: Int,
        val data: JSONObject?,
        val error: String? = null
    )

    data class User(
        val id: String,
        val username: String,
        val displayName: String,
        val avatar: String?,
        val bio: String?,
        val coins: Int,
        val followers: Int,
        val following: Int,
        val verified: Boolean
    )

    data class Session(
        val token: String,
        val user: User?,
        val expiresAt: String?
    )

    data class Gift(
        val id: String,
        val name: String,
        val price: Int,
        val icon: String,
        val imageUrl: String?,
        val animationUrl: String?
    )

    data class MusicTrack(
        val id: String,
        val title: String,
        val artist: String,
        val audioUrl: String,
        val coverUrl: String?,
        val durationSeconds: Int
    )

    data class VisualEffect(
        val id: String,
        val name: String,
        val type: String,
        val value: String
    )

    data class Live(
        val id: String,
        val userId: String,
        val username: String,
        val displayName: String,
        val avatar: String?,
        val title: String,
        val streamUrl: String?,
        val playbackUrl: String?,
        val rtmpsUrl: String?,
        val streamKey: String?,
        val viewerCount: Int,
        val likes: Int,
        val status: String,
        val startedAt: String?
    )

    data class Video(
        val id: String,
        val userId: String,
        val username: String,
        val displayName: String,
        val avatar: String?,
        val videoUrl: String,
        val thumbnailUrl: String?,
        val caption: String,
        val musicName: String?,
        val likes: Int,
        val comments: Int,
        val shares: Int,
        val views: Int,
        val liked: Boolean,
        val createdAt: String?
    )

    data class Wallet(
        val coins: Int,
        val walletNumbers: List<String>,
        val paymentMethods: List<String>
    )

    data class Deposit(
        val id: String,
        val amount: Int,
        val walletNumber: String,
        val transactionReference: String?,
        val coins: Int,
        val status: String,
        val createdAt: String?,
        val updatedAt: String?
    )

    data class GiftSendResult(
        val transactionId: String?,
        val remainingCoins: Int,
        val giftId: String?,
        val giftName: String?,
        val quantity: Int,
        val totalCoins: Int
    )

    private fun saveToken(
        context: Context,
        token: String
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    fun getToken(
        context: Context
    ): String? {
        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                TOKEN_KEY,
                null
            )
    }

    fun clearSession(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(TOKEN_KEY)
            .apply()
    }

    private suspend fun request(
        context: Context,
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true
    ): ApiResult = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null

        try {
            val url = URL(
                BASE_URL.trimEnd('/') +
                    "/" +
                    path.trimStart('/')
            )

            connection =
                url.openConnection()
                    as HttpURLConnection

            connection.requestMethod = method

            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000

            connection.useCaches = false

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )

            if (authenticated) {
                val token =
                    getToken(context)

                if (!token.isNullOrBlank()) {
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )
                }
            }

            if (
                method == "POST" ||
                method == "PATCH" ||
                method == "PUT"
            ) {
                connection.doOutput = true

                val requestBody =
                    body?.toString() ?: "{}"

                connection.outputStream.use { output ->
                    output.write(
                        requestBody.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }
            }

            val statusCode =
                connection.responseCode

            val stream =
                if (statusCode in 200..399) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseText =
                if (stream != null) {
                    BufferedReader(
                        InputStreamReader(
                            stream,
                            Charsets.UTF_8
                        )
                    ).use { reader ->
                        reader.readText()
                    }
                } else {
                    ""
                }

            val json =
                try {
                    if (responseText.isNotBlank()) {
                        JSONObject(responseText)
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }

            val success =
                statusCode in 200..299 &&
                    (
                        json?.optBoolean(
                            "success",
                            true
                        ) ?: true
                    )

            val error =
                if (!success) {
                    json?.optString(
                        "message",
                        null
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "HTTP $statusCode"
                } else {
                    null
                }

            ApiResult(
                success = success,
                statusCode = statusCode,
                data = json,
                error = error
            )

        } catch (e: Exception) {

            ApiResult(
                success = false,
                statusCode = 0,
                data = null,
                error =
                    e.message
                        ?: "Network error"
            )

        } finally {
            connection?.disconnect()
        }
    }

    suspend fun health(
        context: Context
    ): ApiResult {
        return request(
            context = context,
            method = "GET",
            path = "/health",
            authenticated = false
        )
    }

    /*
     * REAL ACCOUNT REGISTRATION
     */

    suspend fun register(
        context: Context,
        username: String,
        password: String,
        displayName: String = ""
    ): Result<Session> {

        val cleanUsername =
            username.trim().lowercase()

        val cleanPassword =
            password

        if (!Regex("^[a-z0-9_]{3,24}$")
                .matches(cleanUsername)
        ) {
            return Result.failure(
                Exception(
                    "Username must be 3-24 characters"
                )
            )
        }

        if (
            cleanPassword.length < 8 ||
            cleanPassword.length > 128
        ) {
            return Result.failure(
                Exception(
                    "Password must be at least 8 characters"
                )
            )
        }

        val body =
            JSONObject().apply {
                put(
                    "username",
                    cleanUsername
                )
                put(
                    "password",
                    cleanPassword
                )

                if (
                    displayName
                        .trim()
                        .isNotBlank()
                ) {
                    put(
                        "displayName",
                        displayName.trim()
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/auth/register",
                body = body,
                authenticated = false
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Registration failed"
                )
            )
        }

        return parseSession(
            context,
            result.data
        )
    }

    /*
     * REAL ACCOUNT LOGIN
     */

    suspend fun login(
        context: Context,
        username: String,
        password: String
    ): Result<Session> {

        val body =
            JSONObject().apply {
                put(
                    "username",
                    username.trim().lowercase()
                )
                put(
                    "password",
                    password
                )
            }

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/auth/login",
                body = body,
                authenticated = false
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Login failed"
                )
            )
        }

        return parseSession(
            context,
            result.data
        )
    }

    /*
     * LOGOUT
     */

    suspend fun logout(
        context: Context
    ): Result<Boolean> {

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/auth/logout"
            )

        clearSession(context)

        return if (result.success) {
            Result.success(true)
        } else {
            Result.failure(
                Exception(
                    result.error
                        ?: "Logout failed"
                )
            )
        }
    }

    private fun parseSession(
        context: Context,
        json: JSONObject?
    ): Result<Session> {

        if (json == null) {
            return Result.failure(
                Exception(
                    "Empty server response"
                )
            )
        }

        val token =
            json.optString("token")

        if (token.isBlank()) {
            return Result.failure(
                Exception(
                    "Session token missing"
                )
            )
        }

        saveToken(
            context,
            token
        )

        val user =
            parseUser(
                json.optJSONObject("user")
            )

        return Result.success(
            Session(
                token = token,
                user = user,
                expiresAt =
                    json.optString(
                        "expiresAt",
                        null
                    )
            )
        )
    }

    /*
     * BACKWARD COMPATIBILITY
     */

    suspend fun createSession(
        context: Context
    ): Result<Session> {

        val existing =
            getToken(context)

        if (!existing.isNullOrBlank()) {
            val current =
                me(context)

            if (current.isSuccess) {
                return Result.success(
                    Session(
                        token = existing,
                        user = current.getOrNull(),
                        expiresAt = null
                    )
                )
            }

            clearSession(context)
        }

        return Result.failure(
            Exception(
                "Please login or create an account"
            )
        )
    }

    suspend fun me(
        context: Context
    ): Result<User> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/me"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load user"
                )
            )
        }

        val user =
            parseUser(
                result.data
                    ?.optJSONObject("user")
            )

        return if (user != null) {
            Result.success(user)
        } else {
            Result.failure(
                Exception(
                    "User data missing"
                )
            )
        }
    }

    /*
     * MUSIC LIBRARY
     */

    suspend fun getMusic(
        context: Context
    ): Result<List<MusicTrack>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/music",
                authenticated = false
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load music"
                )
            )
        }

        val array =
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val items =
            mutableListOf<MusicTrack>()

        for (i in 0 until array.length()) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            val id =
                item.optString("id")

            if (id.isBlank()) {
                continue
            }

            items.add(
                MusicTrack(
                    id = id,
                    title =
                        item.optString(
                            "title"
                        ),
                    artist =
                        item.optString(
                            "artist"
                        ),
                    audioUrl =
                        item.optString(
                            "audio_url"
                        ),
                    coverUrl =
                        item.optString(
                            "cover_url",
                            null
                        ),
                    durationSeconds =
                        item.optInt(
                            "duration_seconds",
                            0
                        )
                )
            )
        }

        return Result.success(items)
    }

    /*
     * VISUAL FILTERS / EFFECTS
     */

    suspend fun getEffects(
        context: Context
    ): Result<List<VisualEffect>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/effects",
                authenticated = false
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load effects"
                )
            )
        }

        val array =
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val items =
            mutableListOf<VisualEffect>()

        for (i in 0 until array.length()) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            val id =
                item.optString("id")

            if (id.isBlank()) {
                continue
            }

            items.add(
                VisualEffect(
                    id = id,
                    name =
                        item.optString(
                            "name"
                        ),
                    type =
                        item.optString(
                            "type"
                        ),
                    value =
                        item.optString(
                            "value"
                        )
                )
            )
        }

        return Result.success(items)
    }

    /*
     * GIFTS
     */

    suspend fun getGifts(
        context: Context
    ): Result<List<Gift>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/gifts"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load gifts"
                )
            )
        }

        val array =
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val gifts =
            mutableListOf<Gift>()

        for (i in 0 until array.length()) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            gifts.add(
                Gift(
                    id =
                        item.optString("id"),
                    name =
                        item.optString("name"),
                    price =
                        item.optInt("price", 0),
                    icon =
                        item.optString(
                            "icon",
                            "🎁"
                        ),
                    imageUrl =
                        item.optString(
                            "imageUrl",
                            null
                        ),
                    animationUrl =
                        item.optString(
                            "animationUrl",
                            null
                        )
                )
            )
        }

        return Result.success(gifts)
    }

    /*
     * FEED
     */

    suspend fun feed(
        context: Context,
        limit: Int = 20,
        cursor: Int = 0
    ): Result<List<Video>> {

        val safeLimit =
            limit.coerceIn(1, 50)

        val safeCursor =
            cursor.coerceAtLeast(0)

        val result =
            request(
                context = context,
                method = "GET",
                path =
                    "/api/feed" +
                        "?limit=$safeLimit" +
                        "&cursor=$safeCursor"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load feed"
                )
            )
        }

        val array =
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val videos =
            mutableListOf<Video>()

        for (i in 0 until array.length()) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            parseVideo(item)
                ?.let {
                    videos.add(it)
                }
        }

        return Result.success(videos)
    }

    /*
     * CREATE VIDEO
     */

    suspend fun createVideo(
        context: Context,
        videoUrl: String,
        caption: String = "",
        thumbnailUrl: String? = null,
        streamId: String? = null,
        musicName: String? = null
    ): Result<String> {

        if (videoUrl.trim().isBlank()) {
            return Result.failure(
                Exception(
                    "Video URL is required"
                )
            )
        }

        val body =
            JSONObject().apply {

                put(
                    "videoUrl",
                    videoUrl.trim()
                )

                put(
                    "caption",
                    caption.trim()
                )

                if (
                    !thumbnailUrl
                        .isNullOrBlank()
                ) {
                    put(
                        "thumbnailUrl",
                        thumbnailUrl
                    )
                }

                if (
                    !streamId
                        .isNullOrBlank()
                ) {
                    put(
                        "streamId",
                        streamId
                    )
                }

                if (
                    !musicName
                        .isNullOrBlank()
                ) {
                    put(
                        "musicName",
                        musicName
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/videos",
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to create video"
                )
            )
        }

        val id =
            result.data
                ?.optString("id")
                ?.takeIf {
                    it.isNotBlank()
                }

        return if (id != null) {
            Result.success(id)
        } else {
            Result.failure(
                Exception(
                    "Video ID missing"
                )
            )
        }
    }

    /*
     * LIKES
     */

    suspend fun likeVideo(
        context: Context,
        videoId: String
    ): Result<Int> {

        val result =
            request(
                context = context,
                method = "POST",
                path =
                    "/api/videos/" +
                        encodePath(videoId) +
                        "/like"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to like video"
                )
            )
        }

        return Result.success(
            result.data
                ?.optInt("likes", 0)
                ?: 0
        )
    }

    suspend fun unlikeVideo(
        context: Context,
        videoId: String
    ): Result<Int> {

        val result =
            request(
                context = context,
                method = "DELETE",
                path =
                    "/api/videos/" +
                        encodePath(videoId) +
                        "/like"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to unlike video"
                )
            )
        }

        return Result.success(
            result.data
                ?.optInt("likes", 0)
                ?: 0
        )
    }

    /*
     * COMMENTS
     */

    suspend fun getComments(
        context: Context,
        videoId: String
    ): Result<JSONArray> {

        val result =
            request(
                context = context,
                method = "GET",
                path =
                    "/api/videos/" +
                        encodePath(videoId) +
                        "/comments"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load comments"
                )
            )
        }

        return Result.success(
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()
        )
    }

    suspend fun addComment(
        context: Context,
        videoId: String,
        text: String
    ): Result<String> {

        if (text.trim().isBlank()) {
            return Result.failure(
                Exception(
                    "Comment cannot be empty"
                )
            )
        }

        val body =
            JSONObject().apply {
                put(
                    "text",
                    text.trim()
                )
            }

        val result =
            request(
                context = context,
                method = "POST",
                path =
                    "/api/videos/" +
                        encodePath(videoId) +
                        "/comments",
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to add comment"
                )
            )
        }

        return Result.success(
            result.data
                ?.optString("id")
                ?: ""
        )
    }

    /*
     * FOLLOW
     */

    suspend fun followUser(
        context: Context,
        userId: String
    ): Result<Boolean> {

        val result =
            request(
                context = context,
                method = "POST",
                path =
                    "/api/users/" +
                        encodePath(userId) +
                        "/follow"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to follow user"
                )
            )
        }

        return Result.success(
            result.data
                ?.optBoolean(
                    "following",
                    true
                )
                ?: true
        )
    }

    suspend fun unfollowUser(
        context: Context,
        userId: String
    ): Result<Boolean> {

        val result =
            request(
                context = context,
                method = "DELETE",
                path =
                    "/api/users/" +
                        encodePath(userId) +
                        "/follow"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to unfollow user"
                )
            )
        }

        return Result.success(
            result.data
                ?.optBoolean(
                    "following",
                    false
                )
                ?: false
        )
    }

    /*
     * UPDATE PROFILE
     */

    suspend fun updateProfile(
        context: Context,
        displayName: String? = null,
        bio: String? = null,
        avatar: String? = null
    ): Result<User> {

        val body =
            JSONObject().apply {

                if (displayName != null) {
                    put(
                        "displayName",
                        displayName.trim()
                    )
                }

                if (bio != null) {
                    put(
                        "bio",
                        bio.trim()
                    )
                }

                if (avatar != null) {
                    put(
                        "avatar",
                        avatar
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "PATCH",
                path = "/api/me",
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to update profile"
                )
            )
        }

        val user =
            parseUser(
                result.data
                    ?.optJSONObject("user")
            )

        return if (user != null) {
            Result.success(user)
        } else {
            Result.failure(
                Exception(
                    "Profile data missing"
                )
            )
        }
    }

    /*
     * LIVE
     */

    suspend fun createLive(
        context: Context,
        title: String,
        streamUrl: String? = null,
        playbackUrl: String? = null,
        rtmpsUrl: String? = null,
        streamKey: String? = null
    ): Result<Live> {

        val body =
            JSONObject().apply {

                put(
                    "title",
                    title.trim()
                )

                if (!streamUrl.isNullOrBlank()) {
                    put(
                        "streamUrl",
                        streamUrl
                    )
                }

                if (!playbackUrl.isNullOrBlank()) {
                    put(
                        "playbackUrl",
                        playbackUrl
                    )
                }

                if (!rtmpsUrl.isNullOrBlank()) {
                    put(
                        "rtmpsUrl",
                        rtmpsUrl
                    )
                }

                if (!streamKey.isNullOrBlank()) {
                    put(
                        "streamKey",
                        streamKey
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/live/create",
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to create live"
                )
            )
        }

        val live =
            parseLive(
                result.data
                    ?.optJSONObject("live")
            )

        return if (live != null) {
            Result.success(live)
        } else {
            Result.failure(
                Exception(
                    "Live data missing"
                )
            )
        }
    }

    suspend fun getLive(
        context: Context,
        liveId: String
    ): Result<Live> {

        val result =
            request(
                context = context,
                method = "GET",
                path =
                    "/api/live/" +
                        encodePath(liveId)
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load live"
                )
            )
        }

        val live =
            parseLive(
                result.data
                    ?.optJSONObject("live")
            )

        return if (live != null) {
            Result.success(live)
        } else {
            Result.failure(
                Exception(
                    "Live data missing"
                )
            )
        }
    }

    suspend fun updateLive(
        context: Context,
        liveId: String,
        status: String? = null,
        title: String? = null
    ): Result<Live> {

        val body =
            JSONObject().apply {

                if (status != null) {
                    put(
                        "status",
                        status
                    )
                }

                if (title != null) {
                    put(
                        "title",
                        title
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "PATCH",
                path =
                    "/api/live/" +
                        encodePath(liveId),
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to update live"
                )
            )
        }

        val live =
            parseLive(
                result.data
                    ?.optJSONObject("live")
            )

        return if (live != null) {
            Result.success(live)
        } else {
            Result.failure(
                Exception(
                    "Live data missing"
                )
            )
        }
    }

    /*
     * SEND GIFT
     */

    suspend fun sendGift(
        context: Context,
        liveId: String,
        giftId: String,
        quantity: Int = 1,
        receiverUserId: String? = null
    ): Result<GiftSendResult> {

        val body =
            JSONObject().apply {

                put(
                    "giftId",
                    giftId
                )

                put(
                    "quantity",
                    quantity.coerceIn(1, 100)
                )

                if (
                    !receiverUserId
                        .isNullOrBlank()
                ) {
                    put(
                        "receiverUserId",
                        receiverUserId
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "POST",
                path =
                    "/api/live/" +
                        encodePath(liveId) +
                        "/gifts",
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to send gift"
                )
            )
        }

        val data =
            result.data
                ?: return Result.failure(
                    Exception(
                        "Gift response missing"
                    )
                )

        val gift =
            data.optJSONObject("gift")

        return Result.success(
            GiftSendResult(
                transactionId =
                    data.optString(
                        "transactionId",
                        null
                    ),
                remainingCoins =
                    data.optInt(
                        "remainingCoins",
                        0
                    ),
                giftId =
                    gift?.optString(
                        "id",
                        null
                    ),
                giftName =
                    gift?.optString(
                        "name",
                        null
                    ),
                quantity =
                    gift?.optInt(
                        "quantity",
                        quantity
                    ) ?: quantity,
                totalCoins =
                    gift?.optInt(
                        "totalCoins",
                        0
                    ) ?: 0
            )
        )
    }

    /*
     * WALLET
     */

    suspend fun getWallet(
        context: Context
    ): Result<Wallet> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/wallet"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load wallet"
                )
            )
        }

        val data =
            result.data
                ?: return Result.failure(
                    Exception(
                        "Wallet response missing"
                    )
                )

        val numbers =
            mutableListOf<String>()

        val numbersArray =
            data.optJSONArray(
                "walletNumbers"
            )

        if (numbersArray != null) {
            for (
                i in 0 until
                    numbersArray.length()
            ) {
                numbers.add(
                    numbersArray.optString(i)
                )
            }
        }

        val methods =
            mutableListOf<String>()

        val methodsArray =
            data.optJSONArray(
                "paymentMethods"
            )

        if (methodsArray != null) {
            for (
                i in 0 until
                    methodsArray.length()
            ) {
                methods.add(
                    methodsArray.optString(i)
                )
            }
        }

        return Result.success(
            Wallet(
                coins =
                    data.optInt(
                        "coins",
                        0
                    ),
                walletNumbers = numbers,
                paymentMethods = methods
            )
        )
    }

    suspend fun createWalletDeposit(
        context: Context,
        amount: Int,
        walletNumber: String,
        transactionReference: String? = null
    ): Result<Deposit> {

        val body =
            JSONObject().apply {

                put(
                    "amount",
                    amount
                )

                put(
                    "walletNumber",
                    walletNumber
                )

                if (
                    !transactionReference
                        .isNullOrBlank()
                ) {
                    put(
                        "transactionReference",
                        transactionReference
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/wallet/deposit",
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to create deposit"
                )
            )
        }

        val data =
            result.data
                ?: return Result.failure(
                    Exception(
                        "Deposit response missing"
                    )
                )

        return Result.success(
            Deposit(
                id =
                    data.optString(
                        "depositId"
                    ),
                amount = amount,
                walletNumber =
                    walletNumber,
                transactionReference =
                    transactionReference,
                coins =
                    data.optInt(
                        "coins",
                        amount
                    ),
                status =
                    data.optString(
                        "status",
                        "pending"
                    ),
                createdAt = null,
                updatedAt = null
            )
        )
    }

    suspend fun getWalletDeposits(
        context: Context
    ): Result<List<Deposit>> {

        val result =
            request(
                context = context,
                method = "GET",
                path =
                    "/api/wallet/deposits"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load deposits"
                )
            )
        }

        val array =
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val deposits =
            mutableListOf<Deposit>()

        for (
            i in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            deposits.add(
                Deposit(
                    id =
                        item.optString("id"),
                    amount =
                        item.optInt(
                            "amount",
                            0
                        ),
                    walletNumber =
                        item.optString(
                            "wallet_number"
                        ),
                    transactionReference =
                        item.optString(
                            "transaction_reference",
                            null
                        ),
                    coins =
                        item.optInt(
                            "coins",
                            0
                        ),
                    status =
                        item.optString(
                            "status",
                            "pending"
                        ),
                    createdAt =
                        item.optString(
                            "created_at",
                            null
                        ),
                    updatedAt =
                        item.optString(
                            "updated_at",
                            null
                        )
                )
            )
        }

        return Result.success(
            deposits
        )
    }

    /*
     * REPORT
     */

    suspend fun report(
        context: Context,
        targetType: String,
        targetId: String,
        reason: String
    ): Result<String> {

        val body =
            JSONObject().apply {
                put(
                    "targetType",
                    targetType
                )
                put(
                    "targetId",
                    targetId
                )
                put(
                    "reason",
                    reason
                )
            }

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/reports",
                body = body
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to submit report"
                )
            )
        }

        return Result.success(
            result.data
                ?.optString(
                    "reportId",
                    ""
                )
                ?: ""
        )
    }

    private fun parseUser(
        json: JSONObject?
    ): User? {

        if (json == null) {
            return null
        }

        val id =
            json.optString("id")

        if (id.isBlank()) {
            return null
        }

        return User(
            id = id,
            username =
                json.optString(
                    "username"
                ),
            displayName =
                json.optString(
                    "displayName"
                ),
            avatar =
                json.optString(
                    "avatar",
                    null
                ),
            bio =
                json.optString(
                    "bio",
                    null
                ),
            coins =
                json.optInt(
                    "coins",
                    0
                ),
            followers =
                json.optInt(
                    "followers",
                    0
                ),
            following =
                json.optInt(
                    "following",
                    0
                ),
            verified =
                json.optBoolean(
                    "verified",
                    false
                )
        )
    }

    private fun parseVideo(
        video: JSONObject?
    ): Video? {

        if (video == null) {
            return null
        }

        val id =
            video.optString("id")

        if (id.isBlank()) {
            return null
        }

        return Video(
            id = id,
            userId =
                video.optString(
                    "userId"
                ),
            username =
                video.optString(
                    "username"
                ),
            displayName =
                video.optString(
                    "displayName"
                ),
            avatar =
                video.optString(
                    "avatar",
                    null
                ),
            videoUrl =
                video.optString(
                    "videoUrl"
                ),
            thumbnailUrl =
                video.optString(
                    "thumbnailUrl",
                    null
                ),
            caption =
                video.optString(
                    "caption"
                ),
            musicName =
                video.optString(
                    "musicName",
                    null
                ),
            likes =
                video.optInt(
                    "likes",
                    0
                ),
            comments =
                video.optInt(
                    "comments",
                    0
                ),
            shares =
                video.optInt(
                    "shares",
                    0
                ),
            views =
                video.optInt(
                    "views",
                    0
                ),
            liked =
                video.optBoolean(
                    "liked",
                    false
                ),
            createdAt =
                video.optString(
                    "createdAt",
                    null
                )
        )
    }

    private fun parseLive(
        live: JSONObject?
    ): Live? {

        if (live == null) {
            return null
        }

        val id =
            live.optString("id")

        if (id.isBlank()) {
            return null
        }

        return Live(
            id = id,
            userId =
                live.optString(
                    "userId"
                ),
            username =
                live.optString(
                    "username"
                ),
            displayName =
                live.optString(
                    "displayName"
                ),
            avatar =
                live.optString(
                    "avatar",
                    null
                ),
            title =
                live.optString(
                    "title"
                ),
            streamUrl =
                live.optString(
                    "streamUrl",
                    null
                ),
            playbackUrl =
                live.optString(
                    "playbackUrl",
                    null
                ),
            rtmpsUrl =
                live.optString(
                    "rtmpsUrl",
                    null
                ),
            streamKey =
                live.optString(
                    "streamKey",
                    null
                ),
            viewerCount =
                live.optInt(
                    "viewerCount",
                    0
                ),
            likes =
                live.optInt(
                    "likes",
                    0
                ),
            status =
                live.optString(
                    "status",
                    "active"
                ),
            startedAt =
                live.optString(
                    "startedAt",
                    null
                )
        )
    }

    private fun encodePath(
        value: String
    ): String {
        return URLEncoder
            .encode(
                value,
                "UTF-8"
            )
            .replace("+", "%20")
    }
}
