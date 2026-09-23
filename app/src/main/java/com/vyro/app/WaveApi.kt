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

    data class GiftSendResult(
        val transactionId: String?,
        val remainingCoins: Int,
        val giftId: String?,
        val giftName: String?,
        val quantity: Int,
        val totalCoins: Int
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

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private fun saveToken(
        context: Context,
        token: String
    ) {
        prefs(context)
            .edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    fun getToken(
        context: Context
    ): String? {
        return prefs(context)
            .getString(
                TOKEN_KEY,
                null
            )
    }

    fun isLoggedIn(
        context: Context
    ): Boolean {
        return !getToken(context).isNullOrBlank()
    }

    fun clearSession(
        context: Context
    ) {
        prefs(context)
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
            val fullUrl =
                BASE_URL.trimEnd('/') +
                    "/" +
                    path.trimStart('/')

            connection =
                URL(fullUrl)
                    .openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.useCaches = false
            connection.doInput = true
            connection.instanceFollowRedirects = true

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )

            connection.setRequestProperty(
                "User-Agent",
                "WaveLive-Android/1.0"
            )

            if (authenticated) {
                getToken(context)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { token ->
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer $token"
                        )
                    }
            }

            if (
                method == "POST" ||
                method == "PUT" ||
                method == "PATCH"
            ) {
                connection.doOutput = true

                val payload =
                    body?.toString() ?: "{}"

                connection.outputStream.use { output ->
                    output.write(
                        payload.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                    output.flush()
                }
            }

            val status =
                connection.responseCode

            val stream =
                if (status in 200..399) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response =
                if (stream != null) {
                    BufferedReader(
                        InputStreamReader(
                            stream,
                            Charsets.UTF_8
                        )
                    ).use {
                        it.readText()
                    }
                } else {
                    ""
                }

            val json =
                if (response.isNotBlank()) {
                    try {
                        JSONObject(response)
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }

            val success =
                status in 200..299 &&
                    (
                        json?.optBoolean(
                            "success",
                            true
                        ) ?: true
                    )

            val message =
                json?.optString(
                    "message",
                    ""
                )?.takeIf {
                    it.isNotBlank()
                }

            ApiResult(
                success = success,
                statusCode = status,
                data = json,
                error =
                    if (success) {
                        null
                    } else {
                        message ?: when (status) {
                            401 -> "Unauthorized"
                            403 -> "Access denied"
                            404 -> "Not found"
                            408 -> "Request timeout"
                            429 -> "Too many requests"
                            in 500..599 ->
                                "Server error"
                            else ->
                                "HTTP $status"
                        }
                    }
            )

        } catch (e: Exception) {

            ApiResult(
                success = false,
                statusCode = 0,
                data = null,
                error =
                    e.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
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

    suspend fun register(
        context: Context,
        username: String,
        password: String,
        displayName: String = ""
    ): Result<Session> {

        val cleanUsername =
            username
                .trim()
                .lowercase()

        if (
            !Regex(
                "^[a-z0-9_]{3,24}$"
            ).matches(cleanUsername)
        ) {
            return Result.failure(
                Exception(
                    "Username must contain 3-24 letters, numbers or _"
                )
            )
        }

        if (
            password.length < 8 ||
            password.length > 128
        ) {
            return Result.failure(
                Exception(
                    "Password must be 8-128 characters"
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
                    password
                )

                val name =
                    displayName.trim()

                if (name.isNotEmpty()) {
                    put(
                        "displayName",
                        name
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

    suspend fun login(
        context: Context,
        username: String,
        password: String
    ): Result<Session> {

        val cleanUsername =
            username
                .trim()
                .lowercase()

        if (cleanUsername.isBlank()) {
            return Result.failure(
                Exception(
                    "Username is required"
                )
            )
        }

        if (password.isBlank()) {
            return Result.failure(
                Exception(
                    "Password is required"
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

            if (result.statusCode == 401) {
                clearSession(context)
            }

            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load account"
                )
            )
        }

        val data =
            result.data

        val userJson =
            data?.optJSONObject("user")
                ?: data

        val user =
            parseUser(userJson)

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

    suspend fun createSession(
        context: Context
    ): Result<Session> {

        val token =
            getToken(context)

        if (token.isNullOrBlank()) {
            return Result.failure(
                Exception(
                    "Please login or create an account"
                )
            )
        }

        val current =
            me(context)

        return if (current.isSuccess) {
            Result.success(
                Session(
                    token = token,
                    user = current.getOrNull(),
                    expiresAt = null
                )
            )
        } else {
            clearSession(context)

            Result.failure(
                Exception(
                    "Session expired"
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
            json.optString(
                "token",
                ""
            ).trim()

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

        val expiresAt =
            if (json.has("expiresAt")) {
                json.optString(
                    "expiresAt",
                    ""
                ).takeIf {
                    it.isNotBlank()
                }
            } else {
                null
            }

        return Result.success(
            Session(
                token = token,
                user = user,
                expiresAt = expiresAt
            )
        )
    }

    suspend fun feed(
        context: Context
    ): Result<List<Video>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/feed",
                authenticated = false
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
            extractArray(
                result.data,
                "items",
                "videos",
                "data"
            )

        val videos =
            mutableListOf<Video>()

        for (
            i in 0 until array.length()
        ) {
            parseVideo(
                array.optJSONObject(i)
            )?.let {
                videos.add(it)
            }
        }

        return Result.success(videos)
    }

    suspend fun likeVideo(
        context: Context,
        videoId: String
    ): Result<Boolean> {

        if (videoId.isBlank()) {
            return Result.failure(
                Exception(
                    "Video ID is required"
                )
            )
        }

        val result =
            request(
                context = context,
                method = "POST",
                path =
                    "/api/videos/" +
                        encodePath(videoId) +
                        "/like"
            )

        return if (result.success) {
            Result.success(true)
        } else {
            Result.failure(
                Exception(
                    result.error
                        ?: "Unable to like video"
                )
            )
        }
    }

    suspend fun viewVideo(
        context: Context,
        videoId: String
    ): Result<Boolean> {

        if (videoId.isBlank()) {
            return Result.failure(
                Exception(
                    "Video ID is required"
                )
            )
        }

        val result =
            request(
                context = context,
                method = "POST",
                path =
                    "/api/videos/" +
                        encodePath(videoId) +
                        "/view",
                authenticated = false
            )

        return if (result.success) {
            Result.success(true)
        } else {
            Result.failure(
                Exception(
                    result.error
                        ?: "Unable to record view"
                )
            )
        }
    }

    suspend fun getLives(
        context: Context
    ): Result<List<Live>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/live",
                authenticated = false
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load live streams"
                )
            )
        }

        val array =
            extractArray(
                result.data,
                "items",
                "lives",
                "data"
            )

        val lives =
            mutableListOf<Live>()

        for (
            i in 0 until array.length()
        ) {
            parseLive(
                array.optJSONObject(i)
            )?.let {
                lives.add(it)
            }
        }

        return Result.success(lives)
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
                        encodePath(liveId),
                authenticated = false
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
                    ?: result.data
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

    suspend fun createLive(
        context: Context,
        title: String
    ): Result<Live> {

        val cleanTitle =
            title.trim()

        if (cleanTitle.isBlank()) {
            return Result.failure(
                Exception(
                    "Live title is required"
                )
            )
        }

        val body =
            JSONObject().apply {
                put(
                    "title",
                    cleanTitle.take(150)
                )
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
                    ?: result.data
            )

        return if (live != null) {
            Result.success(live)
        } else {
            Result.failure(
                Exception(
                    "Live creation response missing"
                )
            )
        }
    }

    suspend fun updateLive(
        context: Context,
        liveId: String,
        status: String? = null,
        title: String? = null,
        viewerCount: Int? = null,
        likes: Int? = null
    ): Result<Live> {

        val body =
            JSONObject().apply {

                status
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        put(
                            "status",
                            it
                        )
                    }

                title
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        put(
                            "title",
                            it.take(150)
                        )
                    }

                viewerCount
                    ?.coerceAtLeast(0)
                    ?.let {
                        put(
                            "viewerCount",
                            it
                        )
                    }

                likes
                    ?.coerceAtLeast(0)
                    ?.let {
                        put(
                            "likes",
                            it
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
                    ?: result.data
            )

        return if (live != null) {
            Result.success(live)
        } else {
            Result.failure(
                Exception(
                    "Updated live data missing"
                )
            )
        }
    }

    suspend fun getGifts(
        context: Context
    ): Result<List<Gift>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/gifts",
                authenticated = false
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
            extractArray(
                result.data,
                "items",
                "gifts",
                "data"
            )

        val gifts =
            mutableListOf<Gift>()

        for (
            i in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            val id =
                item.optString(
                    "id",
                    ""
                )

            if (id.isBlank()) {
                continue
            }

            gifts.add(
                Gift(
                    id = id,
                    name =
                        item.optString(
                            "name",
                            ""
                        ),
                    price =
                        item.optInt(
                            "price",
                            0
                        ),
                    icon =
                        item.optString(
                            "icon",
                            ""
                        ),
                    imageUrl =
                        firstNullableString(
                            item,
                            "imageUrl",
                            "image_url"
                        ),
                    animationUrl =
                        firstNullableString(
                            item,
                            "animationUrl",
                            "animation_url"
                        )
                )
            )
        }

        return Result.success(gifts)
    }

    suspend fun sendGift(
        context: Context,
        liveId: String,
        giftId: String,
        quantity: Int = 1
    ): Result<GiftSendResult> {

        if (liveId.isBlank()) {
            return Result.failure(
                Exception(
                    "Live ID is required"
                )
            )
        }

        if (giftId.isBlank()) {
            return Result.failure(
                Exception(
                    "Gift ID is required"
                )
            )
        }

        val safeQuantity =
            quantity.coerceIn(1, 100)

        val body =
            JSONObject().apply {

                put(
                    "giftId",
                    giftId
                )

                put(
                    "quantity",
                    safeQuantity
                )
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
                    firstNullableString(
                        data,
                        "transactionId",
                        "transaction_id"
                    ),
                remainingCoins =
                    data.optInt(
                        "remainingCoins",
                        0
                    ),
                giftId =
                    firstNullableString(
                        gift ?: JSONObject(),
                        "id"
                    ),
                giftName =
                    firstNullableString(
                        gift ?: JSONObject(),
                        "name"
                    ),
                quantity =
                    gift?.optInt(
                        "quantity",
                        safeQuantity
                    ) ?: safeQuantity,
                totalCoins =
                    gift?.optInt(
                        "totalCoins",
                        0
                    ) ?: 0
            )
        )
    }

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
            extractArray(
                result.data,
                "items",
                "tracks",
                "music",
                "data"
            )

        val tracks =
            mutableListOf<MusicTrack>()

        for (
            i in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            val id =
                item.optString(
                    "id",
                    ""
                )

            if (id.isBlank()) {
                continue
            }

            tracks.add(
                MusicTrack(
                    id = id,
                    title =
                        item.optString(
                            "title",
                            ""
                        ),
                    artist =
                        item.optString(
                            "artist",
                            ""
                        ),
                    audioUrl =
                        firstString(
                            item,
                            "audioUrl",
                            "audio_url"
                        ),
                    coverUrl =
                        firstNullableString(
                            item,
                            "coverUrl",
                            "cover_url"
                        ),
                    durationSeconds =
                        firstInt(
                            item,
                            "durationSeconds",
                            "duration_seconds"
                        )
                )
            )
        }

        return Result.success(tracks)
    }

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
            extractArray(
                result.data,
                "items",
                "effects",
                "data"
            )

        val effects =
            mutableListOf<VisualEffect>()

        for (
            i in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            val id =
                item.optString(
                    "id",
                    ""
                )

            if (id.isBlank()) {
                continue
            }

            effects.add(
                VisualEffect(
                    id = id,
                    name =
                        item.optString(
                            "name",
                            ""
                        ),
                    type =
                        item.optString(
                            "type",
                            ""
                        ),
                    value =
                        item.optString(
                            "value",
                            ""
                        )
                )
            )
        }

        return Result.success(effects)
    }

    suspend fun getUser(
        context: Context,
        userId: String
    ): Result<User> {

        if (userId.isBlank()) {
            return Result.failure(
                Exception(
                    "User ID is required"
                )
            )
        }

        val result =
            request(
                context = context,
                method = "GET",
                path =
                    "/api/users/" +
                        encodePath(userId),
                authenticated = false
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
                    ?: result.data
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

    suspend fun followUser(
        context: Context,
        userId: String
    ): Result<Boolean> {

        if (userId.isBlank()) {
            return Result.failure(
                Exception(
                    "User ID is required"
                )
            )
        }

        val result =
            request(
                context = context,
                method = "POST",
                path =
                    "/api/users/" +
                        encodePath(userId) +
                        "/follow"
            )

        return if (result.success) {
            Result.success(true)
        } else {
            Result.failure(
                Exception(
                    result.error
                        ?: "Unable to follow user"
                )
            )
        }
    }

    suspend fun getComments(
        context: Context,
        videoId: String
    ): Result<List<JSONObject>> {

        val result =
            request(
                context = context,
                method = "GET",
                path =
                    "/api/videos/" +
                        encodePath(videoId) +
                        "/comments",
                authenticated = false
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load comments"
                )
            )
        }

        val array =
            extractArray(
                result.data,
                "items",
                "comments",
                "data"
            )

        val items =
            mutableListOf<JSONObject>()

        for (
            i in 0 until array.length()
        ) {
            array.optJSONObject(i)
                ?.let {
                    items.add(it)
                }
        }

        return Result.success(items)
    }

    suspend fun addComment(
        context: Context,
        videoId: String,
        text: String
    ): Result<JSONObject> {

        val cleanText =
            text.trim()

        if (cleanText.isBlank()) {
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
                    cleanText.take(500)
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
            result.data ?: JSONObject()
        )
    }

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

        return Result.success(
            Wallet(
                coins =
                    data.optInt(
                        "coins",
                        0
                    ),
                walletNumbers =
                    jsonStringList(
                        data.optJSONArray(
                            "walletNumbers"
                        )
                    ),
                paymentMethods =
                    jsonStringList(
                        data.optJSONArray(
                            "paymentMethods"
                        )
                    )
            )
        )
    }

    suspend fun createWalletDeposit(
        context: Context,
        amount: Int,
        walletNumber: String,
        transactionReference: String? = null
    ): Result<Deposit> {

        if (amount <= 0) {
            return Result.failure(
                Exception(
                    "Amount must be greater than zero"
                )
            )
        }

        if (walletNumber.isBlank()) {
            return Result.failure(
                Exception(
                    "Wallet number is required"
                )
            )
        }

        val reference =
            transactionReference
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (reference == null) {
            return Result.failure(
                Exception(
                    "Transaction reference is required"
                )
            )
        }

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

                put(
                    "transactionReference",
                    reference
                )
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
                    firstString(
                        data,
                        "depositId",
                        "deposit_id",
                        "id"
                    ),
                amount =
                    data.optInt(
                        "amount",
                        amount
                    ),
                walletNumber =
                    firstString(
                        data,
                        "walletNumber",
                        "wallet_number"
                    ).ifBlank {
                        walletNumber
                    },
                transactionReference =
                    firstNullableString(
                        data,
                        "transactionReference",
                        "transaction_reference"
                    ) ?: reference,
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
                createdAt =
                    firstNullableString(
                        data,
                        "createdAt",
                        "created_at"
                    ),
                updatedAt =
                    firstNullableString(
                        data,
                        "updatedAt",
                        "updated_at"
                    )
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
                path = "/api/wallet/deposits"
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
            extractArray(
                result.data,
                "items",
                "deposits",
                "data"
            )

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
                        item.optString(
                            "id",
                            ""
                        ),
                    amount =
                        item.optInt(
                            "amount",
                            0
                        ),
                    walletNumber =
                        firstString(
                            item,
                            "walletNumber",
                            "wallet_number"
                        ),
                    transactionReference =
                        firstNullableString(
                            item,
                            "transactionReference",
                            "transaction_reference"
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
                        firstNullableString(
                            item,
                            "createdAt",
                            "created_at"
                        ),
                    updatedAt =
                        firstNullableString(
                            item,
                            "updatedAt",
                            "updated_at"
                        )
                )
            )
        }

        return Result.success(deposits)
    }

    suspend fun getGiftHistory(
        context: Context
    ): Result<List<JSONObject>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/gifts/history"
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to load gift history"
                )
            )
        }

        val array =
            extractArray(
                result.data,
                "items",
                "history",
                "data"
            )

        val items =
            mutableListOf<JSONObject>()

        for (
            i in 0 until array.length()
        ) {
            array.optJSONObject(i)
                ?.let {
                    items.add(it)
                }
        }

        return Result.success(items)
    }

    suspend fun report(
        context: Context,
        targetType: String,
        targetId: String,
        reason: String
    ): Result<String> {

        val cleanType =
            targetType.trim()

        val cleanId =
            targetId.trim()

        val cleanReason =
            reason.trim()

        if (
            cleanType.isBlank() ||
            cleanId.isBlank() ||
            cleanReason.isBlank()
        ) {
            return Result.failure(
                Exception(
                    "Report information is incomplete"
                )
            )
        }

        val body =
            JSONObject().apply {

                put(
                    "targetType",
                    cleanType
                )

                put(
                    "targetId",
                    cleanId
                )

                put(
                    "reason",
                    cleanReason.take(500)
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
            firstString(
                result.data ?: JSONObject(),
                "reportId",
                "report_id",
                "id"
            )
        )
    }

    private fun parseUser(
        json: JSONObject?
    ): User? {

        if (json == null) {
            return null
        }

        val id =
            json.optString(
                "id",
                ""
            )

        if (id.isBlank()) {
            return null
        }

        return User(
            id = id,
            username =
                json.optString(
                    "username",
                    ""
                ),
            displayName =
                firstString(
                    json,
                    "displayName",
                    "display_name"
                ).ifBlank {
                    json.optString(
                        "username",
                        ""
                    )
                },
            avatar =
                firstNullableString(
                    json,
                    "avatar",
                    "avatar_url"
                ),
            bio =
                firstNullableString(
                    json,
                    "bio"
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
        json: JSONObject?
    ): Video? {

        if (json == null) {
            return null
        }

        val id =
            json.optString(
                "id",
                ""
            )

        if (id.isBlank()) {
            return null
        }

        return Video(
            id = id,
            userId =
                firstString(
                    json,
                    "userId",
                    "user_id"
                ),
            username =
                json.optString(
                    "username",
                    ""
                ),
            displayName =
                firstString(
                    json,
                    "displayName",
                    "display_name"
                ).ifBlank {
                    json.optString(
                        "username",
                        ""
                    )
                },
            avatar =
                firstNullableString(
                    json,
                    "avatar",
                    "avatar_url"
                ),
            videoUrl =
                firstString(
                    json,
                    "videoUrl",
                    "video_url"
                ),
            thumbnailUrl =
                firstNullableString(
                    json,
                    "thumbnailUrl",
                    "thumbnail_url"
                ),
            caption =
                json.optString(
                    "caption",
                    ""
                ),
            musicName =
                firstNullableString(
                    json,
                    "musicName",
                    "music_name"
                ),
            likes =
                json.optInt(
                    "likes",
                    0
                ),
            comments =
                json.optInt(
                    "comments",
                    0
                ),
            shares =
                json.optInt(
                    "shares",
                    0
                ),
            views =
                json.optInt(
                    "views",
                    0
                ),
            liked =
                json.optBoolean(
                    "liked",
                    false
                ),
            createdAt =
                firstNullableString(
                    json,
                    "createdAt",
                    "created_at"
                )
        )
    }

    private fun parseLive(
        json: JSONObject?
    ): Live? {

        if (json == null) {
            return null
        }

        val id =
            json.optString(
                "id",
                ""
            )

        if (id.isBlank()) {
            return null
        }

        return Live(
            id = id,
            userId =
                firstString(
                    json,
                    "userId",
                    "user_id"
                ),
            username =
                json.optString(
                    "username",
                    ""
                ),
            displayName =
                firstString(
                    json,
                    "displayName",
                    "display_name"
                ).ifBlank {
                    json.optString(
                        "username",
                        ""
                    )
                },
            avatar =
                firstNullableString(
                    json,
                    "avatar",
                    "avatar_url"
                ),
            title =
                json.optString(
                    "title",
                    ""
                ),
            streamUrl =
                firstNullableString(
                    json,
                    "streamUrl",
                    "stream_url"
                ),
            playbackUrl =
                firstNullableString(
                    json,
                    "playbackUrl",
                    "playback_url"
                ),
            rtmpsUrl =
                firstNullableString(
                    json,
                    "rtmpsUrl",
                    "rtmps_url"
                ),
            streamKey =
                firstNullableString(
                    json,
                    "streamKey",
                    "stream_key"
                ),
            viewerCount =
                firstInt(
                    json,
                    "viewerCount",
                    "viewer_count"
                ),
            likes =
                json.optInt(
                    "likes",
                    0
                ),
            status =
                json.optString(
                    "status",
                    "active"
                ),
            startedAt =
                firstNullableString(
                    json,
                    "startedAt",
                    "started_at"
                )
        )
    }

    private fun extractArray(
        json: JSONObject?,
        vararg keys: String
    ): JSONArray {

        if (json == null) {
            return JSONArray()
        }

        for (key in keys) {
            val array =
                json.optJSONArray(key)

            if (array != null) {
                return array
            }
        }

        return JSONArray()
    }

    private fun firstString(
        json: JSONObject,
        vararg keys: String
    ): String {

        for (key in keys) {

            val value =
                json.optString(
                    key,
                    ""
                )

            if (value.isNotBlank()) {
                return value
            }
        }

        return ""
    }

    private fun firstNullableString(
        json: JSONObject,
        vararg keys: String
    ): String? {

        for (key in keys) {

            if (!json.has(key)) {
                continue
            }

            val value =
                json.optString(
                    key,
                    ""
                )

            if (value.isNotBlank()) {
                return value
            }
        }

        return null
    }

    private fun firstInt(
        json: JSONObject,
        vararg keys: String
    ): Int {

        for (key in keys) {

            if (!json.has(key)) {
                continue
            }

            return json.optInt(
                key,
                0
            )
        }

        return 0
    }

    private fun jsonStringList(
        array: JSONArray?
    ): List<String> {

        if (array == null) {
            return emptyList()
        }

        val result =
            mutableListOf<String>()

        for (
            i in 0 until array.length()
        ) {

            val value =
                array.optString(
                    i,
                    ""
                ).trim()

            if (value.isNotBlank()) {
                result.add(value)
            }
        }

        return result
    }

    private fun encodePath(
        value: String
    ): String {

        return URLEncoder
            .encode(
                value,
                "UTF-8"
            )
            .replace(
                "+",
                "%20"
            )
    }
}
