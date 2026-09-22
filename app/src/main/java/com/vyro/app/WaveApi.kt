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

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
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
                        message ?: "HTTP $status"
                    }
            )
        } catch (e: Exception) {
            ApiResult(
                success = false,
                statusCode = 0,
                data = null,
                error =
                    e.message ?: "Network error"
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
            username.trim().lowercase()

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
                    password
                )

                if (
                    displayName
                        .trim()
                        .isNotEmpty()
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
            clearSession(context)

            return Result.failure(
                Exception(
                    result.error
                        ?: "Session expired"
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
                        json.optJSONObject("user")
                    ),
                expiresAt =
                    json.optString(
                        "expiresAt",
                        null
                    )
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
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val tracks =
            mutableListOf<MusicTrack>()

        for (index in 0 until array.length()) {
            val item =
                array.optJSONObject(index)
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
                        item.optString(
                            "audio_url",
                            item.optString(
                                "audioUrl",
                                ""
                            )
                        ),
                    coverUrl =
                        item.optString(
                            "cover_url",
                            item.optString(
                                "coverUrl",
                                null
                            )
                        ),
                    durationSeconds =
                        item.optInt(
                            "duration_seconds",
                            item.optInt(
                                "durationSeconds",
                                0
                            )
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
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val effects =
            mutableListOf<VisualEffect>()

        for (index in 0 until array.length()) {
            val item =
                array.optJSONObject(index)
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
                            "filter"
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

        for (index in 0 until array.length()) {
            val item =
                array.optJSONObject(index)
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
                            "Gift"
                        ),
                    price =
                        item.optInt(
                            "price",
                            item.optInt(
                                "price_coins",
                                0
                            )
                        ),
                    icon =
                        item.optString(
                            "icon",
                            "G"
                        ),
                    imageUrl =
                        item.optString(
                            "imageUrl",
                            item.optString(
                                "image_url",
                                null
                            )
                        ),
                    animationUrl =
                        item.optString(
                            "animationUrl",
                            item.optString(
                                "animation_url",
                                null
                            )
                        )
                )
            )
        }

        return Result.success(gifts)
    }

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

        for (index in 0 until array.length()) {
            parseVideo(
                array.optJSONObject(index)
            )?.let {
                videos.add(it)
            }
        }

        return Result.success(videos)
    }

    suspend fun getLives(
        context: Context
    ): Result<List<Live>> {

        val result =
            request(
                context = context,
                method = "GET",
                path = "/api/live"
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
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val lives =
            mutableListOf<Live>()

        for (index in 0 until array.length()) {
            parseLive(
                array.optJSONObject(index)
            )?.let {
                lives.add(it)
            }
        }

        return Result.success(lives)
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
                    cleanTitle
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
            JSONObject()

        if (status != null) {
            body.put(
                "status",
                status
            )
        }

        if (title != null) {
            body.put(
                "title",
                title
            )
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

    suspend fun sendGift(
        context: Context,
        liveId: String,
        giftId: String,
        quantity: Int = 1,
        receiverUserId: String? = null
    ): Result<GiftSendResult> {

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

    suspend fun likeVideo(
        context: Context,
        videoId: String
    ): Result<Boolean> {

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
                        "depositId",
                        ""
                    ),
                amount = amount,
                walletNumber = walletNumber,
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
            result.data
                ?.optJSONArray("items")
                ?: JSONArray()

        val deposits =
            mutableListOf<Deposit>()

        for (index in 0 until array.length()) {

            val item =
                array.optJSONObject(index)
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
                        item.optString(
                            "wallet_number",
                            item.optString(
                                "walletNumber",
                                ""
                            )
                        ),
                    transactionReference =
                        item.optString(
                            "transaction_reference",
                            item.optString(
                                "transactionReference",
                                null
                            )
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
                            item.optString(
                                "createdAt",
                                null
                            )
                        ),
                    updatedAt =
                        item.optString(
                            "updated_at",
                            item.optString(
                                "updatedAt",
                                null
                            )
                        )
                )
            )
        }

        return Result.success(deposits)
    }

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
                json.optString(
                    "displayName",
                    json.optString(
                        "display_name",
                        ""
                    )
                ),
            avatar =
                json.optString(
                    "avatar",
                    json.optString(
                        "avatar_url",
                        null
                    )
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
                json.optString(
                    "userId",
                    json.optString(
                        "user_id",
                        ""
                    )
                ),
            username =
                json.optString(
                    "username",
                    ""
                ),
            displayName =
                json.optString(
                    "displayName",
                    json.optString(
                        "display_name",
                        ""
                    )
                ),
            avatar =
                json.optString(
                    "avatar",
                    null
                ),
            videoUrl =
                json.optString(
                    "videoUrl",
                    json.optString(
                        "video_url",
                        ""
                    )
                ),
            thumbnailUrl =
                json.optString(
                    "thumbnailUrl",
                    json.optString(
                        "thumbnail_url",
                        null
                    )
                ),
            caption =
                json.optString(
                    "caption",
                    ""
                ),
            musicName =
                json.optString(
                    "musicName",
                    json.optString(
                        "music_name",
                        null
                    )
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
                json.optString(
                    "createdAt",
                    json.optString(
                        "created_at",
                        null
                    )
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
                json.optString(
                    "userId",
                    json.optString(
                        "user_id",
                        ""
                    )
                ),
            username =
                json.optString(
                    "username",
                    ""
                ),
            displayName =
                json.optString(
                    "displayName",
                    json.optString(
                        "display_name",
                        ""
                    )
                ),
            avatar =
                json.optString(
                    "avatar",
                    null
                ),
            title =
                json.optString(
                    "title",
                    ""
                ),
            streamUrl =
                json.optString(
                    "streamUrl",
                    json.optString(
                        "stream_url",
                        null
                    )
                ),
            playbackUrl =
                json.optString(
                    "playbackUrl",
                    json.optString(
                        "playback_url",
                        null
                    )
                ),
            rtmpsUrl =
                json.optString(
                    "rtmpsUrl",
                    json.optString(
                        "rtmps_url",
                        null
                    )
                ),
            streamKey =
                json.optString(
                    "streamKey",
                    json.optString(
                        "stream_key",
                        null
                    )
                ),
            viewerCount =
                json.optInt(
                    "viewerCount",
                    json.optInt(
                        "viewer_count",
                        0
                    )
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
                json.optString(
                    "startedAt",
                    json.optString(
                        "started_at",
                        null
                    )
                )
        )
    }

    private fun jsonStringList(
        array: JSONArray?
    ): List<String> {

        if (array == null) {
            return emptyList()
        }

        val result =
            mutableListOf<String>()

        for (index in 0 until array.length()) {
            val value =
                array.optString(
                    index,
                    ""
                )

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
