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
            val url =
                BASE_URL.trimEnd('/') +
                    "/" +
                    path.trimStart('/')

            connection =
                URL(url)
                    .openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.useCaches = false
            connection.doInput = true

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

            val error =
                json?.optString(
                    "error",
                    ""
                )?.takeIf {
                    it.isNotBlank()
                } ?: json?.optString(
                    "message",
                    ""
                )?.takeIf {
                    it.isNotBlank()
                } ?: when (status) {
                    401 -> "Unauthorized"
                    403 -> "Access denied"
                    404 -> "Not found"
                    408 -> "Request timeout"
                    429 -> "Too many requests"
                    in 500..599 -> "Server error"
                    0 -> null
                    else -> "HTTP $status"
                }

            ApiResult(
                success = success,
                statusCode = status,
                data = json,
                error = if (success) null else error
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

                if (displayName.trim().isNotBlank()) {
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
            firstString(
                json,
                "token",
                "accessToken",
                "access_token"
            )

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

        return Result.success(
            Session(
                token = token,
                user =
                    parseUser(
                        json.optJSONObject(
                            "user"
                        )
                    ),
                expiresAt =
                    firstNullableString(
                        json,
                        "expiresAt",
                        "expires_at"
                    )
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

    suspend fun feed(
        context: Context,
        limit: Int = 30,
        cursor: Int = 0
    ): Result<List<Video>> {

        val safeLimit =
            limit.coerceIn(
                1,
                100
            )

        val result =
            request(
                context = context,
                method = "GET",
                path =
                    "/api/videos/feed" +
                        "?limit=" +
                        safeLimit +
                        "&cursor=" +
                        cursor,
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
            index in 0 until array.length()
        ) {
            parseVideo(
                array.optJSONObject(index)
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
                        "/view"
            )

        return if (result.success) {
            Result.success(true)
        } else {
            Result.failure(
                Exception(
                    result.error
                        ?: "Unable to register view"
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
                        ?: "Unable to load live rooms"
                )
            )
        }

        val array =
            extractArray(
                result.data,
                "items",
                "lives",
                "live"
            )

        val lives =
            mutableListOf<Live>()

        for (
            index in 0 until array.length()
        ) {
            parseLive(
                array.optJSONObject(index)
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

        if (liveId.isBlank()) {
            return Result.failure(
                Exception(
                    "Live ID is required"
                )
            )
        }

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

        val data =
            result.data

        val live =
            parseLive(
                data?.optJSONObject("live")
                    ?: data
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
                path = "/api/live",
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

        val data =
            result.data

        val live =
            parseLive(
                data?.optJSONObject("live")
                    ?: data
            )

        return if (live != null) {
            Result.success(live)
        } else {
            Result.failure(
                Exception(
                    "Created live data missing"
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

        if (liveId.isBlank()) {
            return Result.failure(
                Exception(
                    "Live ID is required"
                )
            )
        }

        if (
            status == null &&
            title == null
        ) {
            return Result.failure(
                Exception(
                    "Nothing to update"
                )
            )
        }

        val body =
            JSONObject().apply {

                status?.let {
                    put(
                        "status",
                        it
                    )
                }

                title?.let {
                    put(
                        "title",
                        it.trim()
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

        val data =
            result.data

        val live =
            parseLive(
                data?.optJSONObject("live")
                    ?: data
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
                "gifts"
            )

        val gifts =
            mutableListOf<Gift>()

        for (
            index in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(index)
                    ?: continue

            val id =
                firstString(
                    item,
                    "id"
                )

            if (id.isBlank()) {
                continue
            }

            gifts.add(
                Gift(
                    id = id,
                    name =
                        firstString(
                            item,
                            "name"
                        ).ifBlank {
                            "Gift"
                        },
                    price =
                        firstInt(
                            item,
                            "price",
                            "price_coins"
                        ),
                    icon =
                        firstString(
                            item,
                            "icon"
                        ).ifBlank {
                            "🎁"
                        },
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
        quantity: Int = 1,
        receiverUserId: String? = null
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
            quantity.coerceIn(
                1,
                100
            )

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

                receiverUserId
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        put(
                            "receiverUserId",
                            it
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
                ?: JSONObject()

        val gift =
            data.optJSONObject("gift")

        val giftIdResult =
            firstNullableString(
                data,
                "giftId",
                "gift_id"
            ) ?: gift?.optString(
                "id",
                null
            )

        val giftNameResult =
            firstNullableString(
                data,
                "giftName",
                "gift_name"
            ) ?: gift?.optString(
                "name",
                null
            )

        val totalCoins =
            firstInt(
                data,
                "totalCoins",
                "total_coins"
            )

        val remainingCoins =
            firstInt(
                data,
                "remainingCoins",
                "remaining_coins"
            )

        return Result.success(
            GiftSendResult(
                transactionId =
                    firstNullableString(
                        data,
                        "transactionId",
                        "transaction_id"
                    ),
                remainingCoins =
                    remainingCoins,
                giftId =
                    giftIdResult,
                giftName =
                    giftNameResult,
                quantity =
                    data.optInt(
                        "quantity",
                        safeQuantity
                    ),
                totalCoins =
                    totalCoins
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
                "music",
                "tracks"
            )

        val tracks =
            mutableListOf<MusicTrack>()

        for (
            index in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(index)
                    ?: continue

            val id =
                firstString(
                    item,
                    "id"
                )

            if (id.isBlank()) {
                continue
            }

            tracks.add(
                MusicTrack(
                    id = id,
                    title =
                        firstString(
                            item,
                            "title",
                            "name"
                        ),
                    artist =
                        firstString(
                            item,
                            "artist"
                        ),
                    audioUrl =
                        firstString(
                            item,
                            "audioUrl",
                            "audio_url",
                            "url"
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
                            "duration_seconds",
                            "duration"
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
                "effects"
            )

        val effects =
            mutableListOf<VisualEffect>()

        for (
            index in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(index)
                    ?: continue

            val id =
                firstString(
                    item,
                    "id"
                )

            if (id.isBlank()) {
                continue
            }

            effects.add(
                VisualEffect(
                    id = id,
                    name =
                        firstString(
                            item,
                            "name"
                        ).ifBlank {
                            "Effect"
                        },
                    type =
                        firstString(
                            item,
                            "type"
                        ),
                    value =
                        firstString(
                            item,
                            "value"
                        )
                )
            )
        }

        return Result.success(effects)
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
                ?: JSONObject()

        return Result.success(
            Wallet(
                coins =
                    firstInt(
                        data,
                        "coins",
                        "balance"
                    ),
                walletNumbers =
                    jsonStringList(
                        data.optJSONArray(
                            "walletNumbers"
                        )
                            ?: data.optJSONArray(
                                "wallet_numbers"
                            )
                    ),
                paymentMethods =
                    jsonStringList(
                        data.optJSONArray(
                            "paymentMethods"
                        )
                            ?: data.optJSONArray(
                                "payment_methods"
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

                transactionReference
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        put(
                            "transactionReference",
                            it.trim()
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
                ?: JSONObject()

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
                    firstInt(
                        data,
                        "amount"
                    ).let {
                        if (it > 0) it else amount
                    },
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
                    ) ?: transactionReference,
                coins =
                    firstInt(
                        data,
                        "coins"
                    ),
                status =
                    firstString(
                        data,
                        "status"
                    ).ifBlank {
                        "pending"
                    },
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
                "deposits"
            )

        val deposits =
            mutableListOf<Deposit>()

        for (
            index in 0 until array.length()
        ) {

            val item =
                array.optJSONObject(index)
                    ?: continue

            deposits.add(
                Deposit(
                    id =
                        firstString(
                            item,
                            "id",
                            "depositId",
                            "deposit_id"
                        ),
                    amount =
                        firstInt(
                            item,
                            "amount"
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
                        firstInt(
                            item,
                            "coins"
                        ),
                    status =
                        firstString(
                            item,
                            "status"
                        ).ifBlank {
                            "pending"
                        },
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

        val data =
            result.data

        val user =
            parseUser(
                data?.optJSONObject("user")
                    ?: data
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

        val array =
            extractArray(
                result.data,
                "items",
                "comments"
            )

        val list =
            mutableListOf<JSONObject>()

        for (
            index in 0 until array.length()
        ) {
            array.optJSONObject(index)
                ?.let {
                    list.add(it)
                }
        }

        return Result.success(list)
    }

    suspend fun addComment(
        context: Context,
        videoId: String,
        text: String
    ): Result<JSONObject> {

        val cleanText =
            text.trim()

        if (
            videoId.isBlank() ||
            cleanText.isBlank()
        ) {
            return Result.failure(
                Exception(
                    "Comment information is incomplete"
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

        return if (result.success) {
            Result.success(
                result.data ?: JSONObject()
            )
        } else {
            Result.failure(
                Exception(
                    result.error
                        ?: "Unable to add comment"
                )
            )
        }
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
                "history"
            )

        val list =
            mutableListOf<JSONObject>()

        for (
            index in 0 until array.length()
        ) {
            array.optJSONObject(index)
                ?.let {
                    list.add(it)
                }
        }

        return Result.success(list)
    }

    suspend fun report(
        context: Context,
        targetType: String,
        targetId: String,
        reason: String
    ): Result<String> {

        if (
            targetType.isBlank() ||
            targetId.isBlank() ||
            reason.trim().isBlank()
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
                    targetType
                )

                put(
                    "targetId",
                    targetId
                )

                put(
                    "reason",
                    reason.trim().take(500)
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
            firstString(
                json,
                "id",
                "userId",
                "user_id"
            )

        if (id.isBlank()) {
            return null
        }

        return User(
            id = id,

            username =
                firstString(
                    json,
                    "username"
                ),

            displayName =
                firstString(
                    json,
                    "displayName",
                    "display_name"
                ).ifBlank {
                    firstString(
                        json,
                        "username"
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
                firstInt(
                    json,
                    "coins",
                    "balance"
                ),

            followers =
                firstInt(
                    json,
                    "followers",
                    "followersCount",
                    "followers_count"
                ),

            following =
                firstInt(
                    json,
                    "following",
                    "followingCount",
                    "following_count"
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
            firstString(
                json,
                "id",
                "videoId",
                "video_id"
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
                firstString(
                    json,
                    "username"
                ),

            displayName =
                firstString(
                    json,
                    "displayName",
                    "display_name"
                ).ifBlank {
                    firstString(
                        json,
                        "username"
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
                    "video_url",
                    "url"
                ),

            thumbnailUrl =
                firstNullableString(
                    json,
                    "thumbnailUrl",
                    "thumbnail_url",
                    "thumbnail"
                ),

            caption =
                firstString(
                    json,
                    "caption",
                    "description"
                ),

            musicName =
                firstNullableString(
                    json,
                    "musicName",
                    "music_name"
                ),

            likes =
                firstInt(
                    json,
                    "likes",
                    "likeCount",
                    "like_count"
                ),

            comments =
                firstInt(
                    json,
                    "comments",
                    "commentCount",
                    "comment_count"
                ),

            shares =
                firstInt(
                    json,
                    "shares",
                    "shareCount",
                    "share_count"
                ),

            views =
                firstInt(
                    json,
                    "views",
                    "viewCount",
                    "view_count"
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
            firstString(
                json,
                "id",
                "liveId",
                "live_id"
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
                firstString(
                    json,
                    "username"
                ),

            displayName =
                firstString(
                    json,
                    "displayName",
                    "display_name"
                ).ifBlank {
                    firstString(
                        json,
                        "username"
                    )
                },

            avatar =
                firstNullableString(
                    json,
                    "avatar",
                    "avatar_url"
                ),

            title =
                firstString(
                    json,
                    "title"
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
                    "playback_url",
                    "playback"
                ),

            rtmpsUrl =
                firstNullableString(
                    json,
                    "rtmpsUrl",
                    "rtmps_url",
                    "rtmpUrl",
                    "rtmp_url"
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
                    "viewer_count",
                    "viewers"
                ),

            likes =
                firstInt(
                    json,
                    "likes",
                    "likeCount",
                    "like_count"
                ),

            status =
                firstString(
                    json,
                    "status"
                ).ifBlank {
                    "active"
                },

            startedAt =
                firstNullableString(
                    json,
                    "startedAt",
                    "started_at"
                )
        )
    }

    private fun firstString(
        json: JSONObject,
        vararg keys: String
    ): String {

        for (key in keys) {

            if (!json.has(key)) {
                continue
            }

            val value =
                json.optString(
                    key,
                    ""
                ).trim()

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
                ).trim()

            if (
                value.isNotBlank() &&
                value.lowercase() != "null"
            ) {
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

            val value =
                json.optInt(
                    key,
                    Int.MIN_VALUE
                )

            if (value != Int.MIN_VALUE) {
                return value
            }

            val text =
                json.optString(
                    key,
                    ""
                )

            text.toIntOrNull()
                ?.let {
                    return it
                }
        }

        return 0
    }

    private fun extractArray(
        json: JSONObject?,
        vararg keys: String
    ): JSONArray {

        if (json == null) {
            return JSONArray()
        }

        for (key in keys) {

            val direct =
                json.optJSONArray(key)

            if (direct != null) {
                return direct
            }

            val nested =
                json.optJSONObject(key)

            if (nested != null) {

                val nestedArray =
                    nested.optJSONArray("items")
                        ?: nested.optJSONArray("data")
                        ?: nested.optJSONArray("results")

                if (nestedArray != null) {
                    return nestedArray
                }
            }
        }

        return JSONArray()
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
            index in 0 until array.length()
        ) {

            val value =
                array.optString(
                    index,
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
