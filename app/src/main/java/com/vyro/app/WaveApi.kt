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

/**
 * Wave Live API client
 *
 * Worker:
 * https://worker-jolly-band-100e.mha19941024.workers.dev/
 *
 * لا يحتاج هذا الملف إلى Retrofit أو OkHttp.
 * يستخدم HttpURLConnection الموجودة داخل Android.
 */
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

    /**
     * حفظ التوكن.
     */
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

    /**
     * قراءة التوكن.
     */
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

    /**
     * حذف الجلسة.
     */
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

    /**
     * تنفيذ HTTP request.
     */
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
                    (json?.optBoolean(
                        "success",
                        true
                    ) ?: true)

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

    /**
     * فحص السيرفر.
     *
     * GET /health
     */
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

    /**
     * إنشاء جلسة مستخدم جديدة.
     *
     * POST /api/session
     */
    suspend fun createSession(
        context: Context
    ): Result<Session> {

        val result =
            request(
                context = context,
                method = "POST",
                path = "/api/session",
                authenticated = false
            )

        if (!result.success) {
            return Result.failure(
                Exception(
                    result.error
                        ?: "Unable to create session"
                )
            )
        }

        val json =
            result.data
                ?: return Result.failure(
                    Exception("Empty server response")
                )

        val token =
            json.optString("token")

        if (token.isBlank()) {
            return Result.failure(
                Exception("Session token missing")
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

        val session =
            Session(
                token = token,
                user = user,
                expiresAt =
                    json.optString(
                        "expiresAt",
                        null
                    )
            )

        return Result.success(session)
    }

    /**
     * الحصول على المستخدم الحالي.
     *
     * GET /api/me
     */
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
                Exception("User data missing")
            )
        }
    }

    /**
     * الحصول على الفيديوهات.
     *
     * GET /api/feed
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

    /**
     * إنشاء فيديو.
     *
     * POST /api/videos
     */
    suspend fun createVideo(
        context: Context,
        videoUrl: String,
        caption: String = "",
        thumbnailUrl: String? = null,
        streamId: String? = null,
        musicName: String? = null
    ): Result<String> {

        val body =
            JSONObject().apply {

                put(
                    "videoUrl",
                    videoUrl
                )

                put(
                    "caption",
                    caption
                )

                if (!thumbnailUrl.isNullOrBlank()) {
                    put(
                        "thumbnailUrl",
                        thumbnailUrl
                    )
                }

                if (!streamId.isNullOrBlank()) {
                    put(
                        "streamId",
                        streamId
                    )
                }

                if (!musicName.isNullOrBlank()) {
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
                Exception("Video ID missing")
            )
        }
    }

    /**
     * إعجاب.
     *
     * POST /api/videos/{id}/like
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

    /**
     * إزالة الإعجاب.
     *
     * DELETE /api/videos/{id}/like
     */
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

    /**
     * الحصول على التعليقات.
     *
     * GET /api/videos/{id}/comments
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

    /**
     * إضافة تعليق.
     *
     * POST /api/videos/{id}/comments
     */
    suspend fun addComment(
        context: Context,
        videoId: String,
        text: String
    ): Result<String> {

        if (text.trim().isBlank()) {
            return Result.failure(
                Exception("Comment cannot be empty")
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

    /**
     * متابعة مستخدم.
     *
     * POST /api/users/{id}/follow
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

    /**
     * إلغاء المتابعة.
     *
     * DELETE /api/users/{id}/follow
     */
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

        return Result.success(false)
    }

    /**
     * تحديث الملف الشخصي.
     *
     * PATCH /api/profile
     */
    suspend fun updateProfile(
        context: Context,
        displayName: String? = null,
        bio: String? = null,
        avatarUrl: String? = null
    ): Result<User> {

        val body =
            JSONObject().apply {

                if (displayName != null) {
                    put(
                        "displayName",
                        displayName
                    )
                }

                if (bio != null) {
                    put(
                        "bio",
                        bio
                    )
                }

                if (avatarUrl != null) {
                    put(
                        "avatarUrl",
                        avatarUrl
                    )
                }
            }

        val result =
            request(
                context = context,
                method = "PATCH",
                path = "/api/profile",
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
                Exception("Updated user data missing")
            )
        }
    }

    /**
     * الهدايا.
     *
     * GET /api/gifts
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
                            ""
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

    /**
     * إنشاء بث مباشر.
     *
     * POST /api/live/create
     */
    suspend fun createLive(
        context: Context,
        title: String
    ): Result<Live> {

        val cleanTitle =
            title.trim()

        if (cleanTitle.isBlank()) {
            return Result.failure(
                Exception("Live title is required")
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
                Exception("Live data missing")
            )
        }
    }

    /**
     * الحصول على بث معين.
     *
     * GET /api/live/{id}
     */
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
                Exception("Live data missing")
            )
        }
    }

    /**
     * تحديث حالة البث.
     *
     * PATCH /api/live/{id}
     */
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
                Exception("Live data missing")
            )
        }
    }

    /**
     * إرسال هدية أثناء البث.
     *
     * POST /api/live/{id}/gifts
     */
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

                if (!receiverUserId.isNullOrBlank()) {
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

        val json =
            result.data
                ?: return Result.failure(
                    Exception("Empty gift response")
                )

        val gift =
            json.optJSONObject("gift")

        return Result.success(
            GiftSendResult(
                transactionId =
                    json.optString(
                        "transactionId",
                        null
                    ),
                remainingCoins =
                    json.optInt(
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
                    )
                        ?: safeQuantity,
                totalCoins =
                    gift?.optInt(
                        "totalCoins",
                        0
                    )
                        ?: 0
            )
        )
    }

    /**
     * معلومات المحفظة.
     *
     * GET /api/wallet
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

        val json =
            result.data
                ?: return Result.failure(
                    Exception("Empty wallet response")
                )

        val numbersArray =
            json.optJSONArray(
                "walletNumbers"
            )
                ?: JSONArray()

        val numbers =
            mutableListOf<String>()

        for (i in 0 until numbersArray.length()) {
            numbers.add(
                numbersArray.optString(i)
            )
        }

        val methodsArray =
            json.optJSONArray(
                "paymentMethods"
            )
                ?: JSONArray()

        val methods =
            mutableListOf<String>()

        for (i in 0 until methodsArray.length()) {
            methods.add(
                methodsArray.optString(i)
            )
        }

        return Result.success(
            Wallet(
                coins =
                    json.optInt(
                        "coins",
                        0
                    ),
                walletNumbers =
                    numbers,
                paymentMethods =
                    methods
            )
        )
    }

    /**
     * إرسال طلب شحن للمحفظة.
     *
     * POST /api/wallet/deposit
     *
     * ملاحظة:
     * السيرفر يضع الطلب Pending ولا يضيف
     * العملات حتى يتم التحقق من الدفع.
     */
    suspend fun createWalletDeposit(
        context: Context,
        amount: Int,
        walletNumber: String,
        transactionReference: String? = null
    ): Result<String> {

        if (amount <= 0) {
            return Result.failure(
                Exception("Invalid amount")
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

                if (!transactionReference.isNullOrBlank()) {
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
                        ?: "Unable to submit deposit"
                )
            )
        }

        val depositId =
            result.data
                ?.optString(
                    "depositId"
                )
                ?.takeIf {
                    it.isNotBlank()
                }

        return if (depositId != null) {
            Result.success(depositId)
        } else {
            Result.failure(
                Exception("Deposit ID missing")
            )
        }
    }

    /**
     * سجل عمليات الشحن.
     *
     * GET /api/wallet/deposits
     */
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

        for (i in 0 until array.length()) {

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
                            "walletNumber"
                        ),
                    transactionReference =
                        item.optString(
                            "transactionReference",
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
                            "createdAt",
                            null
                        ),
                    updatedAt =
                        item.optString(
                            "updatedAt",
                            null
                        )
                )
            )
        }

        return Result.success(deposits)
    }

    /**
     * إرسال بلاغ.
     *
     * POST /api/reports
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

    /**
     * تحويل JSONObject إلى User.
     */
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
                    "username",
                    ""
                ),
            displayName =
                json.optString(
                    "displayName",
                    ""
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

    /**
     * تحويل JSONObject إلى Video.
     */
    private fun parseVideo(
        json: JSONObject?
    ): Video? {

        if (json == null) {
            return null
        }

        val id =
            json.optString("id")

        if (id.isBlank()) {
            return null
        }

        return Video(
            id = id,
            userId =
                json.optString(
                    "userId",
                    ""
                ),
            username =
                json.optString(
                    "username",
                    ""
                ),
            displayName =
                json.optString(
                    "displayName",
                    ""
                ),
            avatar =
                json.optString(
                    "avatar",
                    null
                ),
            videoUrl =
                json.optString(
                    "videoUrl",
                    ""
                ),
            thumbnailUrl =
                json.optString(
                    "thumbnailUrl",
                    null
                ),
            caption =
                json.optString(
                    "caption",
                    ""
                ),
            musicName =
                json.optString(
                    "musicName",
                    null
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
                    null
                )
        )
    }

    /**
     * تحويل JSONObject إلى Live.
     */
    private fun parseLive(
        json: JSONObject?
    ): Live? {

        if (json == null) {
            return null
        }

        val id =
            json.optString("id")

        if (id.isBlank()) {
            return null
        }

        return Live(
            id = id,
            userId =
                json.optString(
                    "userId",
                    ""
                ),
            username =
                json.optString(
                    "username",
                    ""
                ),
            displayName =
                json.optString(
                    "displayName",
                    ""
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
                    null
                ),
            playbackUrl =
                json.optString(
                    "playbackUrl",
                    null
                ),
            rtmpsUrl =
                json.optString(
                    "rtmpsUrl",
                    null
                ),
            streamKey =
                json.optString(
                    "streamKey",
                    null
                ),
            viewerCount =
                json.optInt(
                    "viewerCount",
                    0
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
                    null
                )
        )
    }

    /**
     * حماية أجزاء URL.
     */
    private fun encodePath(
        value: String
    ): String {
        return java.net.URLEncoder
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
