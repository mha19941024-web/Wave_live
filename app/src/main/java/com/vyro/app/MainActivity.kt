package com.vyro.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE =
    "https://worker-jolly-band-100e.mha19941024.workers.dev"

private val WavePurple = Color(0xFF7C4DFF)
private val WaveDark = Color(0xFF08080D)
private val WaveCard = Color(0xFF15151D)

data class WaveUser(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = ""
)

data class WaveVideo(
    val id: String = "",
    val url: String = "",
    val user: String = "",
    val caption: String = "",
    val likes: Int = 0,
    val liked: Boolean = false
)

data class ApiResult(
    val code: Int,
    val body: String
)

object WaveApi {

    private suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null
    ): ApiResult = withContext(Dispatchers.IO) {

        try {
            val connection =
                URL(API_BASE + path)
                    .openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.useCaches = false

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (!token.isNullOrBlank()) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (body != null) {
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.outputStream.use { output ->
                    output.write(
                        body.toByteArray(Charsets.UTF_8)
                    )
                }
            }

            val code =
                connection.responseCode

            val input =
                if (code in 200..399) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response =
                input
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

            connection.disconnect()

            ApiResult(
                code = code,
                body = response
            )

        } catch (e: Exception) {

            ApiResult(
                code = -1,
                body = e.message ?: "Network error"
            )
        }
    }

    suspend fun createSession(): ApiResult {
        return request(
            method = "POST",
            path = "/api/session",
            body = "{}"
        )
    }

    suspend fun me(
        token: String
    ): ApiResult {
        return request(
            method = "GET",
            path = "/api/me",
            token = token
        )
    }

    suspend fun feed(
        token: String
    ): ApiResult {
        return request(
            method = "GET",
            path = "/api/feed?limit=20",
            token = token
        )
    }

    suspend fun like(
        token: String,
        videoId: String,
        liked: Boolean
    ): ApiResult {

        return if (liked) {

            request(
                method = "POST",
                path = "/api/videos/$videoId/like",
                token = token,
                body = "{}"
            )

        } else {

            request(
                method = "DELETE",
                path = "/api/videos/$videoId/like",
                token = token
            )
        }
    }

    suspend fun createLive(
        token: String,
        title: String
    ): ApiResult {

        val body =
            JSONObject()
                .put(
                    "title",
                    title
                )
                .toString()

        return request(
            method = "POST",
            path = "/api/live/create",
            token = token,
            body = body
        )
    }

    suspend fun updateProfile(
        token: String,
        displayName: String,
        bio: String
    ): ApiResult {

        val body =
            JSONObject()
                .put(
                    "displayName",
                    displayName
                )
                .put(
                    "bio",
                    bio
                )
                .toString()

        return request(
            method = "PATCH",
            path = "/api/profile",
            token = token,
            body = body
        )
    }

    /*
     * الخطوة الأولى:
     * الحصول على Upload URL من الـWorker.
     */
    private suspend fun createUploadTicket(
        token: String
    ): ApiResult {

        return request(
            method = "POST",
            path = "/api/upload/direct",
            token = token,
            body = "{}"
        )
    }

    /*
     * رفع الفيديو الحقيقي إلى Cloudflare Stream.
     *
     * Worker يرجع:
     * result.uploadURL
     * result.uid
     */
    suspend fun uploadVideo(
        context: Context,
        token: String,
        uri: Uri
    ): ApiResult = withContext(Dispatchers.IO) {

        try {

            val ticket =
                createUploadTicket(token)

            if (ticket.code !in 200..299) {

                return@withContext ticket
            }

            val ticketJson =
                JSONObject(ticket.body)

            val result =
                ticketJson.optJSONObject(
                    "result"
                )

            if (result == null) {

                return@withContext ApiResult(
                    502,
                    "Upload ticket is invalid"
                )
            }

            val uploadUrl =
                result.optString(
                    "uploadURL"
                )

            val uid =
                result.optString(
                    "uid"
                )

            if (
                uploadUrl.isBlank() ||
                uid.isBlank()
            ) {

                return@withContext ApiResult(
                    502,
                    "Cloudflare upload ticket is incomplete"
                )
            }

            val input =
                context.contentResolver
                    .openInputStream(uri)

                    ?: return@withContext ApiResult(
                        400,
                        "Cannot read selected video"
                    )

            val boundary =
                "----WaveLiveBoundary" +
                    System.currentTimeMillis()

            val connection =
                URL(uploadUrl)
                    .openConnection()
                        as HttpURLConnection

            connection.requestMethod =
                "POST"

            connection.doOutput =
                true

            connection.useCaches =
                false

            connection.connectTimeout =
                30000

            connection.readTimeout =
                180000

            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            val header =
                "--$boundary\r\n" +
                    "Content-Disposition: form-data; " +
                    "name=\"file\"; " +
                    "filename=\"wave_video.mp4\"\r\n" +
                    "Content-Type: video/mp4\r\n\r\n"

            connection.outputStream.use { output ->

                output.write(
                    header.toByteArray(
                        Charsets.UTF_8
                    )
                )

                input.use { source ->

                    val buffer =
                        ByteArray(64 * 1024)

                    var count =
                        source.read(buffer)

                    while (count != -1) {

                        output.write(
                            buffer,
                            0,
                            count
                        )

                        count =
                            source.read(buffer)
                    }
                }

                val ending =
                    "\r\n--$boundary--\r\n"

                output.write(
                    ending.toByteArray(
                        Charsets.UTF_8
                    )
                )

                output.flush()
            }

            val uploadCode =
                connection.responseCode

            val responseStream =
                if (
                    uploadCode in
                    200..299
                ) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseBody =
                responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

            connection.disconnect()

            if (
                uploadCode in
                200..299
            ) {

                return@withContext ApiResult(
                    200,
                    JSONObject()
                        .put(
                            "uid",
                            uid
                        )
                        .toString()
                )
            }

            ApiResult(
                uploadCode,
                responseBody
            )

        } catch (e: Exception) {

            ApiResult(
                -1,
                e.message ?: "Video upload failed"
            )
        }
    }

    /*
     * بعد نجاح الرفع، نسجل streamId في قاعدة البيانات.
     */
    suspend fun createVideo(
        token: String,
        streamId: String,
        caption: String
    ): ApiResult {

        val body =
            JSONObject()
                .put(
                    "streamId",
                    streamId
                )
                .put(
                    "caption",
                    caption
                )
                .toString()

        return request(
            method = "POST",
            path = "/api/videos",
            token = token,
            body = body
        )
    }

    suspend fun gifts(): ApiResult {

        return request(
            method = "GET",
            path = "/api/gifts"
        )
    }
}

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContent {

            MaterialTheme {

                WaveApp()
            }
        }
    }
}

@Composable
private fun WaveApp() {

    var token by remember {
        mutableStateOf("")
    }

    var user by remember {
        mutableStateOf<WaveUser?>(null)
    }

    var videos by remember {
        mutableStateOf<List<WaveVideo>>(
            emptyList()
        )
    }

    var selectedTab by remember {
        mutableStateOf(0)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val scope =
        rememberCoroutineScope()

    LaunchedEffect(Unit) {

        val session =
            WaveApi.createSession()

        if (
            session.code !in
            200..299
        ) {

            errorMessage =
                "تعذر الاتصال بخادم Wave Live"

            loading = false

            return@LaunchedEffect
        }

        try {

            val sessionJson =
                JSONObject(
                    session.body
                )

            token =
                sessionJson.optString(
                    "token"
                )

            val sessionUser =
                sessionJson.optJSONObject(
                    "user"
                )

            if (sessionUser != null) {

                user =
                    parseUser(
                        sessionUser
                    )
            }

            if (
                token.isNotBlank()
            ) {

                val meResult =
                    WaveApi.me(token)

                if (
                    meResult.code in
                    200..299
                ) {

                    val meJson =
                        JSONObject(
                            meResult.body
                        )

                    val meUser =
                        meJson.optJSONObject(
                            "user"
                        ) ?: meJson

                    user =
                        parseUser(
                            meUser
                        )
                }

                val feedResult =
                    WaveApi.feed(token)

                if (
                    feedResult.code in
                    200..299
                ) {

                    val feedJson =
                        JSONObject(
                            feedResult.body
                        )

                    val array =
                        feedJson.optJSONArray(
                            "items"
                        )
                            ?: feedJson.optJSONArray(
                                "videos"
                            )
                            ?: feedJson.optJSONArray(
                                "data"
                            )

                    videos =
                        parseVideos(array)
                }
            }

        } catch (e: Exception) {

            errorMessage =
                e.message
                    ?: "حدث خطأ أثناء تشغيل التطبيق"
        }

        loading = false
   
