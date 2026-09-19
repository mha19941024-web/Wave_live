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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallet
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

data class WaveUser(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = ""
)

data class WaveVideo(
    val id: String,
    val url: String,
    val user: String,
    val caption: String,
    val likes: Int,
    val liked: Boolean
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
                URL(API_BASE + path).openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/json")

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
                connection.outputStream.use {
                    it.write(body.toByteArray())
                }
            }

            val code = connection.responseCode

            val stream =
                if (code in 200..399) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response =
                stream?.bufferedReader()?.use { it.readText() } ?: ""

            connection.disconnect()

            ApiResult(code, response)

        } catch (e: Exception) {
            ApiResult(
                -1,
                e.message ?: "Network error"
            )
        }
    }

    suspend fun createSession(): ApiResult =
        request(
            "POST",
            "/api/session",
            body = "{}"
        )

    suspend fun me(token: String): ApiResult =
        request(
            "GET",
            "/api/me",
            token
        )

    suspend fun feed(token: String): ApiResult =
        request(
            "GET",
            "/api/feed?limit=20",
            token
        )

    suspend fun like(
        token: String,
        videoId: String,
        liked: Boolean
    ): ApiResult {
        return if (liked) {
            request(
                "POST",
                "/api/videos/$videoId/like",
                token,
                "{}"
            )
        } else {
            request(
                "DELETE",
                "/api/videos/$videoId/like",
                token
            )
        }
    }

    suspend fun createLive(
        token: String,
        title: String
    ): ApiResult =
        request(
            "POST",
            "/api/live/create",
            token,
            JSONObject()
                .put("title", title)
                .toString()
        )

    suspend fun updateProfile(
        token: String,
        displayName: String,
        bio: String
    ): ApiResult =
        request(
            "PATCH",
            "/api/profile",
            token,
            JSONObject()
                .put("displayName", displayName)
                .put("bio", bio)
                .toString()
        )

    suspend fun createVideo(
        token: String,
        url: String,
        caption: String
    ): ApiResult =
        request(
            "POST",
            "/api/videos",
            token,
            JSONObject()
                .put("url", url)
                .put("caption", caption)
                .toString()
        )

    suspend fun gifts(): ApiResult =
        request(
            "GET",
            "/api/gifts"
        )
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WaveRoot()
            }
        }
    }
}

@Composable
private fun WaveRoot() {

    var token by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<WaveUser?>(null) }
    var videos by remember { mutableStateOf<List<WaveVideo>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {

        val session = WaveApi.createSession()

        if (session.code !in 200..299) {
            error = "تعذر الاتصال بخادم Wave Live"
            loading = false
            return@LaunchedEffect
        }

        try {

            val json = JSONObject(session.body)

            val newToken =
                json.optString("token")

            token = newToken

            val userObject =
                json.optJSONObject("user")

            if (userObject != null) {
                user = parseUser(userObject)
            }

            val meResult =
                WaveApi.me(newToken)

            if (meResult.code in 200..299) {

                val meJson =
                    JSONObject(meResult.body)

                val meObject =
                    meJson.optJSONObject("user")
                        ?: meJson

                user = parseUser(meObject)
            }

            val feedResult =
                WaveApi.feed(newToken)

            if (feedResult.code in 200..299) {

                val feedJson =
                    JSONObject(feedResult.body)

                val array =
                    feedJson.optJSONArray("videos")
                        ?: feedJson.optJSONArray("data")

                videos = parseVideos(array)
            }

        } catch (e: Exception) {

            error =
                e.message ?: "حدث خطأ أثناء تشغيل التطبيق"
        }

        loading = false
    }

    Scaffold(
        containerColor = WaveDark,
        bottomBar = {

            if (!loading) {

                WaveBottomBar(
                    selected = selectedTab,
                    onSelected = {
                        selectedTab = it
                    }
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WaveDark)
                .padding(padding)
        ) {

            if (loading) {

                LoadingScreen()

            } else {

                when (selectedTab) {

                    0 -> HomeScreen(
                        videos = videos,
                        onLike = { video ->

                            val currentToken =
                                token ?: return@HomeScreen

                            val newLiked =
                                !video.liked

                            videos =
                                videos.map {

                                    if (it.id == video.id) {

                                        it.copy(
                                            liked = newLiked,
                                            likes =
                                                if (newLiked) {
                                                    it.likes + 1
                                                } else {
                                                    maxOf(
                                                        0,
                                                        it.likes - 1
                                                    )
                                                }
                                        )

                                    } else {
                                        it
                                    }
                                }

                            scope.launch {

                                WaveApi.like(
                                    currentToken,
                                    video.id,
                                    newLiked
                                )
                            }
                        }
                    )

                    1 -> LiveScreen(
                        token = token ?: ""
                    )

                    2 -> CreateScreen(
                        token = token ?: "",
                        onPublished = {

                            scope.launch {

                                val result =
                                    WaveApi.feed(
                                        token ?: ""
                                    )

                                if (result.code in 200..299) {

                                    try {

                                        val json =
                                            JSONObject(result.body)

                                        videos =
                                            parseVideos(
                                                json.optJSONArray(
                                                    "videos"
                                                )
                                                    ?: json.optJSONArray(
                                                        "data"
                                                    )
                                            )

                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
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

            if (error.isNotBlank()) {

                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3A1725)
                    )
                ) {

                    Text(
                        text = error,
                        color = Color.White,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "WAVE",
            color = WavePurple,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "جارٍ تحميل Wave Live...",
            color = Color.White
        )
    }
}

@Composable
private fun WaveBottomBar(
    selected: Int,
    onSelected: (Int) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BottomItem(
            icon = Icons.Default.Home,
            text = "الرئيسية",
            selected = selected == 0
        ) {
            onSelected(0)
        }

        BottomItem(
            icon = Icons.Default.Videocam,
            text = "LIVE",
            selected = selected == 1
        ) {
            onSelected(1)
        }

        BottomItem(
            icon = Icons.Default.Add,
            text = "إنشاء",
            selected = selected == 2
        ) {
            onSelected(2)
        }

        BottomItem(
            icon = Icons.Default.Chat,
            text = "الوارد",
            selected = selected == 3
        ) {
            onSelected(3)
        }

        BottomItem(
            icon = Icons.Default.Person,
            text = "حسابي",
            selected = selected == 4
        ) {
            onSelected(4)
        }
    }
}

@Composable
private fun BottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = text,
            tint =
                if (selected) {
                    WavePurple
                } else {
                    Color.Gray
                }
        )

        Text(
            text = text,
            color =
                if (selected) {
                    Color.White
                } else {
                    Color.Gray
                },
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HomeScreen(
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
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(16.dp)
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

            Text(
                text = "WAVE",
                color = WavePurple,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
            )
        }

        items(
            items = videos,
            key = { it.id }
        ) { video ->

            VideoCard(
                video = video,
                onLike = {
                    onLike(video)
                }
            )
        }
    }
}

@Composable
private fun VideoCard(
    video: WaveVideo,
    onLike: () -> Unit
) {

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal
