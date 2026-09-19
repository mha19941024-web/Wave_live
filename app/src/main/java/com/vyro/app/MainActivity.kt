package com.vyro.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE =
    "https://worker-jolly-band-100e.mha19941024.workers.dev"

private const val PREFS = "wave_live_session"
private const val TOKEN_KEY = "token"

private val WaveBlack = Color(0xFF07060B)
private val WaveSurface = Color(0xFF14111B)
private val WavePurple = Color(0xFF9B5CFF)
private val WavePink = Color(0xFFFF4FA3)
private val WaveGold = Color(0xFFFFC857)
private val WaveGreen = Color(0xFF20D889)

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

    private fun open(
        method: String,
        path: String,
        token: String? = null
    ): HttpURLConnection {
        val connection =
            URL(API_BASE + path).openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )
        }

        return connection
    }

    suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null
    ): ApiResult = withContext(Dispatchers.IO) {

        val connection = open(method, path, token)

        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )

            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val code = connection.responseCode

        val stream =
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val text = stream?.bufferedReader()?.use {
            it.readText()
        } ?: ""

        connection.disconnect()

        ApiResult(code, text)
    }

    suspend fun createSession(): ApiResult {
        return request(
            method = "POST",
            path = "/api/session"
        )
    }

    suspend fun getMe(token: String): ApiResult {
        return request(
            method = "GET",
            path = "/api/me",
            token = token
        )
    }

    suspend fun getFeed(token: String): ApiResult {
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
        return request(
            method = if (liked) "DELETE" else "POST",
            path = "/api/videos/$videoId/like",
            token = token
        )
    }

    suspend fun follow(
        token: String,
        username: String,
        following: Boolean
    ): ApiResult {
        return request(
            method = if (following) "DELETE" else "POST",
            path = "/api/users/$username/follow",
            token = token
        )
    }

    suspend fun updateProfile(
        token: String,
        displayName: String,
        bio: String
    ): ApiResult {
        val json = JSONObject()
            .put("displayName", displayName)
            .put("bio", bio)

        return request(
            method = "PATCH",
            path = "/api/profile",
            token = token,
            body = json.toString()
        )
    }

    suspend fun createLive(
        token: String,
        title: String
    ): ApiResult {
        val json = JSONObject()
            .put("title", title)

        return request(
            method = "POST",
            path = "/api/live/create",
            token = token,
            body = json.toString()
        )
    }

    suspend fun getGifts(token: String): ApiResult {
        return request(
            method = "GET",
            path = "/api/gifts",
            token = token
        )
    }

    suspend fun publishVideo(
        token: String,
        url: String,
        caption: String
    ): ApiResult {
        val json = JSONObject()
            .put("url", url)
            .put("caption", caption)

        return request(
            method = "POST",
            path = "/api/videos",
            token = token,
            body = json.toString()
        )
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaveTheme {
                WaveApp()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WaveTheme(
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = WavePurple,
            secondary = WavePink,
            background = WaveBlack,
            surface = WaveSurface
        ),
        content = content
    )
}

@androidx.compose.runtime.Composable
private fun WaveApp() {

    val context = LocalContext.current

    var token by remember {
        mutableStateOf(
            context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(TOKEN_KEY, null)
        )
    }

    var user by remember {
        mutableStateOf(WaveUser())
    }

    var videos by remember {
        mutableStateOf<List<WaveVideo>>(emptyList())
    }

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        try {

            if (token.isNullOrBlank()) {

                val result = WaveApi.createSession()

                if (result.code !in 200..299) {
                    errorMessage = "تعذر الاتصال بخادم Wave"
                    loading = false
                    return@LaunchedEffect
                }

                val json = JSONObject(result.body)
                val newToken = json.optString("token")

                if (newToken.isBlank()) {
                    errorMessage = "الخادم لم يرجع جلسة صحيحة"
                    loading = false
                    return@LaunchedEffect
                }

                token = newToken

                context
                    .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                    )
                    .edit()
                    .putString(TOKEN_KEY, newToken)
                    .apply()
            }

            val activeToken = token ?: ""

            val meResult = WaveApi.getMe(activeToken)

            if (meResult.code in 200..299) {

                val json = JSONObject(meResult.body)
                    .optJSONObject("user")

                if (json != null) {
                    user = parseUser(json)
                }
            }

            val feedResult = WaveApi.getFeed(activeToken)

            if (feedResult.code in 200..299) {

                val items = JSONObject(feedResult.body)
                    .optJSONArray("items")

                videos = parseVideos(items)
            } else {
                errorMessage = "تعذر تحميل الفيديوهات"
            }

        } catch (e: Exception) {
            errorMessage = e.message ?: "حدث خطأ غير معروف"
        }

        loading = false
    }

    Scaffold(
        containerColor = WaveBlack,
        bottomBar = {

            NavigationBar(
                containerColor = WaveSurface,
                modifier = Modifier.navigationBarsPadding()
            ) {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "الرئيسية"
                        )
                    },
                    label = { Text("الرئيسية") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.LiveTv,
                            contentDescription = "LIVE"
                        )
                    },
                    label = { Text("LIVE") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "إنشاء"
                        )
                    },
                    label = { Text("إنشاء") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = "الوارد"
                        )
                    },
                    label = { Text("الوارد") }
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "الملف"
                        )
                    },
                    label = { Text("ملفي") }
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WaveBlack)
                .padding(padding)
        ) {

            if (loading) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "WAVE",
                        color = WavePurple,
                        fontSize = 42.sp
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    LinearProgressIndicator(
                        modifier = Modifier.width(180.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "جارٍ تحميل Wave Live...",
                        color = Color.White
                    )
                }

            } else {

                when (selectedTab) {

                    0 -> HomeScreen(
                        token = token ?: "",
                        videos = videos,
                        onLike = { video ->

                            val currentToken = token ?: ""

                            videos = videos.map {
                                if (it.id == video.id) {
                                    it.copy(
                                        liked = !it.liked,
                                        likes = if (it.liked) {
                                            maxOf(0, it.likes - 1)
                                        } else {
                                            it.likes + 1
                                        }
                                    )
                                } else {
                                    it
                                }
                            }

                            kotlinx.coroutines.GlobalScope.launch(
                                Dispatchers.IO
                            ) {
                                WaveApi.like(
                                    currentToken,
                                    video.id,
                                    video.liked
                                )
                            }
                        }
                    )

                    1 -> LiveScreen(
                        token = token ?: ""
                    )

                    2 -> CreateScreen(
                        token = token ?: ""
                    )

                    3 -> InboxScreen()

                    4 -> ProfileScreen(
                        token = token ?: "",
                        user = user,
                        onUserChanged = {
                            user = it
                        }
                    )
                }
            }

            if (errorMessage.isNotBlank()) {

                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3A1725)
                    )
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

private fun parseUser(
    json: JSONObject
): WaveUser {

    return WaveUser(
        id = json.optString("id"),
        username = json.optString("username"),
        displayName = json.optString("displayName"),
        bio = json.optString("bio"),
        avatarUrl = json.optString("avatarUrl")
    )
}

private fun parseVideos(
    array: JSONArray?
): List<WaveVideo> {

    if (array == null) {
        return emptyList()
    }

    val result = mutableListOf<WaveVideo>()

    for (i in 0 until array.length()) {

        val item = array.optJSONObject(i)
            ?: continue

        result += WaveVideo(
            id = item.optString("id"),
            url = item.optString("url"),
            user = item.optString("user"),
            caption = item.optString("caption"),
            likes = item.optInt("likes"),
            liked = item.optBoolean("liked", false)
        )
    }

    return result
}

@androidx.compose.runtime.Composable
private fun HomeScreen(
    token: String,
    videos: List<WaveVideo>,
    onLike: (WaveVideo) -> Unit
) {

    if (videos.isEmpty()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "WAVE",
                color = WavePurple,
                fontSize = 40.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "لا توجد فيديوهات منشورة حتى الآن",
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "ابدأ بنشر أول فيديو في Wave Live",
                color = Color.LightGray
            )
        }

        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {

        item {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "WAVE",
                    color = WavePurple,
                    fontSize = 28.sp
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.White
