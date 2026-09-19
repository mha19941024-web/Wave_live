package com.vyro.app

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
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
private val WaveText = Color(0xFFF5F5F7)

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

data class WaveGift(
    val id: String,
    val name: String,
    val price: Int,
    val icon: String
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

                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val responseCode = connection.responseCode

            val stream =
                if (responseCode in 200..399) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseBody =
                stream?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

            connection.disconnect()

            ApiResult(
                code = responseCode,
                body = responseBody
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

    suspend fun getGifts(): ApiResult {
        return request(
            method = "GET",
            path = "/api/gifts"
        )
    }

    suspend fun likeVideo(
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

        val body = JSONObject()
            .put("title", title)
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

        val body = JSONObject()
            .put("displayName", displayName)
            .put("bio", bio)
            .toString()

        return request(
            method = "PATCH",
            path = "/api/profile",
            token = token,
            body = body
        )
    }

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

            val root =
                JSONObject(ticket.body)

            val result =
                root.optJSONObject("result")
                    ?: root

            val uploadUrl =
                result.optString("uploadURL")

            val uid =
                result.optString("uid")

            if (uploadUrl.isBlank() || uid.isBlank()) {

                return@withContext ApiResult(
                    502,
                    "Invalid upload ticket"
                )
            }

            val input =
                context.contentResolver.openInputStream(uri)
                    ?: return@withContext ApiResult(
                        400,
                        "Cannot read selected video"
                    )

            val boundary =
                "----WaveLiveBoundary${System.currentTimeMillis()}"

            val connection =
                URL(uploadUrl).openConnection()
                    as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.useCaches = false
            connection.connectTimeout = 30000
            connection.readTimeout = 180000

            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            val header =
                "--$boundary\r\n" +
                "Content-Disposition: form-data; " +
                "name=\"file\"; filename=\"wave_video.mp4\"\r\n" +
                "Content-Type: video/mp4\r\n\r\n"

            connection.outputStream.use { output ->

                output.write(
                    header.toByteArray(Charsets.UTF_8)
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

                output.write(
                    "\r\n--$boundary--\r\n"
                        .toByteArray(Charsets.UTF_8)
                )

                output.flush()
            }

            val code =
                connection.responseCode

            val stream =
                if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response =
                stream?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

            connection.disconnect()

            if (code in 200..299) {

                ApiResult(
                    200,
                    JSONObject()
                        .put("uid", uid)
                        .toString()
                )

            } else {

                ApiResult(
                    code,
                    response
                )
            }

        } catch (e: Exception) {

            ApiResult(
                -1,
                e.message ?: "Video upload failed"
            )
        }
    }

    suspend fun createVideo(
        token: String,
        streamId: String,
        caption: String
    ): ApiResult {

        val body = JSONObject()
            .put("streamId", streamId)
            .put("caption", caption)
            .toString()

        return request(
            method = "POST",
            path = "/api/videos",
            token = token,
            body = body
        )
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

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
        mutableStateOf(WaveUser())
    }

    var videos by remember {
        mutableStateOf(emptyList<WaveVideo>())
    }

    var selectedTab by remember {
        mutableStateOf(0)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var message by remember {
        mutableStateOf("")
    }

    val scope =
        rememberCoroutineScope()

    LaunchedEffect(Unit) {

        val session =
            WaveApi.createSession()

        if (session.code !in 200..299) {

            message =
                "تعذر الاتصال بخادم Wave Live"

            loading = false
            return@LaunchedEffect
        }

        try {

            val json =
                JSONObject(session.body)

            token =
                json.optString("token")

            json.optJSONObject("user")
                ?.let {
                    user =
                        parseUser(it)
                }

            if (token.isNotBlank()) {

                val me =
                    WaveApi.getMe(token)

                if (me.code in 200..299) {

                    val meJson =
                        JSONObject(me.body)

                    user =
                        parseUser(
                            meJson.optJSONObject("user")
                                ?: meJson
                        )
                }

                val feed =
                    WaveApi.getFeed(token)

                if (feed.code in 200..299) {

                    videos =
                        parseVideos(
                            JSONObject(feed.body)
                        )
                }
            }

        } catch (e: Exception) {

            message =
                e.message ?: "حدث خطأ"
        }

        loading = false
    }

    Scaffold(
        containerColor = WaveDark,

        bottomBar = {

            WaveBottomBar(
                selected = selectedTab,
                onSelect = {
                    selectedTab = it
                }
            )
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WaveDark)
                .padding(paddingValues)
        ) {

            when (selectedTab) {

                0 -> {

                    HomeScreen(
                        videos = videos,
                        loading = loading,
                        message = message,
                        token = token,
                        scope = scope
                    ) { updated ->

                        videos = updated
                    }
                }

                1 -> {

                    LiveScreen(
                        token = token,
                        scope = scope
                    )
                }

                2 -> {

                    UploadScreen(
                        token = token,
                        scope = scope
                    ) {
                        selectedTab = 0
                    }
                }

                3 -> {

                    WalletScreen()
                }

                4 -> {

                    ProfileScreen(
                        user = user,
                        token = token,
                        scope = scope
                    ) { updated ->

                        user = updated
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101017))
            .navigationBarsPadding()
            .padding(vertical = 8.dp),

        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {

        BottomItem(
            Icons.Default.Home,
            "الرئيسية",
            selected == 0
        ) {
            onSelect(0)
        }

        BottomItem(
            Icons.Default.Videocam,
            "LIVE",
            selected == 1
        ) {
            onSelect(1)
        }

        BottomItem(
            Icons.Default.Add,
            "رفع",
            selected == 2
        ) {
            onSelect(2)
        }

        BottomItem(
            Icons.Default.Wallet,
            "المحفظة",
            selected == 3
        ) {
            onSelect(3)
        }

        BottomItem(
            Icons.Default.Person,
            "حسابي",
            selected == 4
        ) {
            onSelect(4)
        }
    }
}

@Composable
private fun BottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(horizontal = 10.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = title,
            tint =
                if (selected) {
                    WavePurple
                } else {
                    Color.Gray
                },

            modifier =
                Modifier.size(25.dp)
        )

        Text(
            text = title,
            color =
                if (selected) {
                    WaveText
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
    loading: Boolean,
    message: String,
    token: String,
    scope: CoroutineScope,
    onVideos: (List<WaveVideo>) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(WaveDark)
    ) {

        Text(
            text = "WAVE",
            color = WaveText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.padding(18.dp)
        )

        when {

            loading -> {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "جارٍ تحميل الفيديوهات…",
                        color = Color.White
                    )
                }
            }

            videos.isEmpty() -> {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        if (message.isBlank()) {
                            "لا توجد فيديوهات بعد"
                        } else {
                            message
                        },

                        color = Color.LightGray
                    )
                }
            }

            else -> {

                LazyColumn {

                    items(
                        videos,
                        key = {
                            it.id
                        }
                    ) { video ->

                        VideoCard(
                            video = video,
                            token = token,
                            scope = scope
                        ) { changed ->

                            onVideos(
                                videos.map {
                                    if (it.id == changed.id) {
                                        changed
                                    } else {
                                        it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: WaveVideo,
    token: String,
    scope: CoroutineScope,
    onChanged: (WaveVideo) -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                ),

        colors =
            CardDefaults.cardColors(
                containerColor = WaveCard
            ),

        shape =
            RoundedCornerShape(18.dp)
    ) {

        Column {

            if (video.url.isNotBlank()) {

                AndroidView(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(420.dp),

                    factory = { context ->

                        VideoView(context).apply {

                            setVideoURI(
                                Uri.parse(video.url)
                            )

                            setMediaController(
                                MediaController(context)
                            )

                            setOnPreparedListener {
                                it.isLooping = true
                            }
                        }
                    },

                    update = {

                        it.setVideoURI(
                            Uri.parse(video.url)
                        )

                        it.start()
                    }
                )

            } else {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color.Black),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "الفيديو غير متاح",
                        color = Color.Gray
                    )
                }
            }

            Text(
                text =
                    video.user.ifBlank {
                        "Wave Creator"
                    },

                color = Color.White,
                fontWeight = FontWeight.Bold,

                modifier =
                    Modifier.padding(
                        start = 14.dp,
                        top = 10.dp
                    )
            )

            if (video.caption.isNotBlank()) {

                Text(
                    text = video.caption,
                    color = Color.LightGray,

                    modifier =
                        Modifier.padding(
                            14.dp,
                            4.dp
                        )
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),

                horizontalArrangement =
                    Arrangement.End
            ) {

                IconButton(

                    onClick = {

                        val newLiked =
                            !video.liked

                        val newLikes =
                            (
                                video.likes +
                                    if (newLiked) 1 else -1
                                )
                                .coerceAtLeast(0)

                        onChanged(
                            video.copy(
                                liked = newLiked,
                                likes = newLikes
                            )
                        )

                        scope.launch {

                            WaveApi.likeVideo(
                                token,
                                video.id,
                                newLiked
                            )
                        }
                    }
                ) {

                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "إعجاب",

                        tint =
                            if (video.liked) {
                                Color.Red
                            } else {
                                Color.White
                            }
                    )
                }

                Text(
                    text =
                        video.likes.toString(),

                    color = Color.White,

                    modifier =
                        Modifier.align(
                            Alignment.CenterVertically
                        )
                )
            }
        }
    }
}

@Composable
private fun UploadScreen(
    token: String,
    scope: CoroutineScope,
    onDone: () -> Unit
) {

    val context =
        LocalContext.current

    var videoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var caption by remember {
        mutableStateOf("")
    }

    var status by remember {
        mutableStateOf("")
    }

    var uploading by remember {
        mutableStateOf(false)
    }

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            videoUri = uri

            status =
                if (uri != null) {
                    "تم اختيار الفيديو"
                } else {
                    ""
                }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            "رفع فيديو",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(25.dp)
        )

        Button(
            onClick = {
                picker.launch("video/*")
            },

            colors =
                ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
        ) {

            Text(
                if (videoUri == null) {
                    "اختيار فيديو"
                } else {
                    "تغيير الفيديو"
                }
            )
        }

        Spacer(
            Modifier.height(15.dp)
        )

        OutlinedTextField(
            value = caption,
            onValueChange = {
                caption = it
            },

            label = {
                Text("الوصف")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Button(

            enabled =
                videoUri != null &&
                    !uploading &&
                    token.isNotBlank(),

            onClick = {

                uploading = true

                status =
                    "جارٍ رفع الفيديو إلى الخادم…"

                scope.launch {

                    val result =
                        WaveApi.uploadVideo(
                            context,
                            token,
                            videoUri!!
                        )

                    if (result.code in 200..299) {

                        val uid =
                            JSONObject(result.body)
                                .optString("uid")

                        status =
                            "تم الرفع. جارٍ نشر الفيديو…"

                        val created =
                            WaveApi.createVideo(
                                token,
                                uid,
                                caption
                            )

                        uploading = false

                        if (created.code in 200..299) {

                            status =
                                "تم نشر الفيديو بنجاح"

                            onDone()

                        } else {

                            status =
                                "تم رفع الملف ولكن فشل تسجيل المنشور: HTTP ${created.code}"
                        }

                    } else {

                        uploading = false

                        status =
                            "فشل الرفع: ${result.body.take(180)}"
                    }
                }
            },

            colors =
                ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
        ) {

            Text(
                if (uploading) {
                    "جارٍ الرفع…"
                } else {
                    "نشر الفيديو"
                }
            )
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            status,
            color = Color.LightGray
        )
    }
}

@Composable
private fun LiveScreen(
    token: String,
    scope: CoroutineScope
) {

    var title by remember {
        mutableStateOf("")
    }

    var result by remember {
        mutableStateOf("")
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp)
    ) {

        Text(
            "البث المباشر",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
            },

            label = {
                Text("عنوان البث")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(15.dp)
        )

        Button(

            enabled =
                token.isNotBlank() &&
                    title.isNotBlank(),

            onClick = {

                scope.launch {

                    val response =
                        WaveApi.createLive(
                            token,
                            title
                        )

                    result =
                        if (response.code in 200..299) {
                            "تم إنشاء جلسة البث من الخادم."
                        } else {
                            "فشل إنشاء البث: HTTP ${response.code}"
                        }
                }
            },

            colors =
                ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
        ) {

            Text("بدء البث")
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            result,
            color = Color.LightGray
        )

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "هذه الشاشة تنشئ جلسة البث على الخادم. إرسال صورة الكاميرا والصوت فعليًا يحتاج طبقة بث RTMP/WebRTC داخل التطبيق.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun WalletScreen() {

    var showDeposit by remember {
        mutableStateOf(false)
    }

    var showGifts by remember {
        mutableStateOf(false)
    }

    var gifts by remember {
        mutableStateOf(emptyList<WaveGift>())
    }

    val scope =
        rememberCoroutineScope()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp)
    ) {

        Text(
            "المحفظة",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(20.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            colors =
                CardDefaults.cardColors(
                    containerColor = WaveCard
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(20.dp)
            ) {

                Text(
                    "Wave Coins",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "رصيد العملات سيظهر عند ربط رصيد المستخدم من الخادم.",
                    color = Color.Gray,

                    modifier =
                        Modifier.padding(
                            top = 8.dp
                        )
                )
            }
        }

        Spacer(
            Modifier.height(15.dp)
        )

        Button(
            onClick = {
                showDeposit = true
            },

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
        ) {

            Text(
                "إيداع عبر المحافظ الإلكترونية"
            )
        }

        Spacer(
            Modifier.height(10.dp)
        )

        Button(

            onClick = {

                scope.launch {

                    val response =
                        WaveApi.getGifts()

                    if (response.code in 200..299) {

                        gifts =
                            parseGifts(
                                response.body
                            )
                    }

                    showGifts = true
                }
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("الهدايا")
        }
    }

    if (showDeposit) {

        AlertDialog(

            onDismissRequest = {
                showDeposit = false
            },

            title = {
                Text(
                    "إيداع Wave Coins"
                )
            },

            text = {

                Text(
                    "الدفع بالمحافظ الإلكترونية فقط.\n\n" +
                        "أرقام التحويل المحددة للمشروع:\n" +
                        "01284306120\n" +
                        "01144210918\n\n" +
                        "يجب تأكيد التحويل من الخادم قبل إضافة العملات إلى الرصيد."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showDeposit = false
                    }
                ) {

                    Text("تم")
                }
            }
        )
    }

    if (showGifts) {

        AlertDialog(

            onDismissRequest = {
                showGifts = false
            },

            title = {
                Text("هدايا Wave")
            },

            text = {

                Column {

                    if (gifts.isEmpty()) {

                        Text(
                            "لا توجد هدايا محملة من الخادم."
                        )

                    } else {

                        gifts.forEach { gift ->

                            Text(
                                "${gift.icon}  ${gift.name} — ${gift.price} Coins",

                                modifier =
                                    Modifier.padding(
                                        vertical = 5.dp
                                    )
                            )
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showGifts = false
                    }
                ) {

                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun ProfileScreen(
    user: WaveUser,
    token: String,
    scope: CoroutineScope,
    onSaved: (WaveUser) -> Unit
) {

    var name by remember(
        user.displayName
    ) {
        mutableStateOf(
            user.displayName
        )
    }

    var bio by remember(
        user.bio
    ) {
        mutableStateOf(
            user.bio
        )
    }

    var status by remember {
        mutableStateOf("")
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp)
    ) {

        Box(
            modifier =
                Modifier
                    .size(90.dp)
                    .background(
                        WavePurple,
                        CircleShape
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    (
                        name.ifBlank {
                            "W"
                        }
                        )
                        .take(1)
                        .uppercase(),

                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "حسابي",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },

            label = {
                Text("الاسم")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = bio,
            onValueChange = {
                bio = it
            },

            label = {
                Text("النبذة")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(16.dp)
        )

        Button(

            enabled =
                token.isNotBlank(),

            onClick = {

                scope.launch {

                    val response =
                        WaveApi.updateProfile(
                            token,
                            name,
                            bio
                        )

                    if (response.code in 200..299) {

                        onSaved(
                            user.copy(
                                displayName = name,
                                bio = bio
                            )
                        )

                        status =
                            "تم حفظ الملف الشخصي"

                    } else {

                        status =
                            "فشل الحفظ: HTTP ${response.code}"
                    }
                }
            },

            colors =
                ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
        ) {

            Text(
                "حفظ الملف الشخصي"
            )
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            status,
            color = Color.LightGray
        )
    }
}

private fun parseUser(
    json: JSONObject
): WaveUser {

    return WaveUser(

        id =
            json.optString("id"),

        username =
            json.optString("username"),

        displayName =
            json.optString("displayName")
                .ifBlank {
                    json.optString("name")
                },

        bio =
            json.optString("bio"),

        avatarUrl =
            json.optString("avatarUrl")
                .ifBlank {
                    json.optString("avatar")
                }
    )
}

private fun parseVideos(
    root: JSONObject
): List<WaveVideo> {

    val array =
        root.optJSONArray("items")
            ?: root.optJSONArray("videos")
            ?: root.optJSONArray("data")
            ?: JSONArray()

    val result =
        mutableListOf<WaveVideo>()

    for (index in 0 until array.length()) {

        val json =
            array.optJSONObject(index)
                ?: continue

        result += WaveVideo(

            id =
                json.optString("id")
                    .ifBlank {
                        json.optString("videoId")
                    },

            url =
                json.optString("url")
                    .ifBlank {
                        json.optString("playbackUrl")
                            .ifBlank {
                                json.optString("videoUrl")
                            }
                    },

            user =
                json.optString("username")
                    .ifBlank {
                        json.optString("displayName")
                            .ifBlank {
                                json.optString("user")
                            }
                    },

            caption =
                json.optString("caption")
                    .ifBlank {
                        json.optString("description")
                    },

            likes =
                json.optInt(
                    "likes",
                    json.optInt(
                        "likeCount",
                        0
                    )
                ),

            liked =
                json.optBoolean(
                    "liked",
                    false
                )
        )
    }

    return result
}

private fun parseGifts(
    body: String
): List<WaveGift> {

    return try {

        val root =
            JSONObject(body)

        val array =
            root.optJSONArray("items")
                ?: root.optJSONArray("gifts")
                ?: root.optJSONArray("data")
                ?: JSONArray()

        buildList {

            for (index in 0 until array.length()) {

                val json =
                    array.optJSONObject(index)
                        ?: continue

                add(
                    WaveGift(

                        id =
                            json.optString("id"),

                        name =
                            json.optString("name")
                                .ifBlank {
                                    "Gift"
                                },

                        price =
                            json.optInt(
                                "price",
                                json.optInt(
                                    "coins",
                                    0
                                )
                            ),

                        icon =
                            json.optString("icon")
                                .ifBlank {
                                    "🎁"
                                }
                    )
                )
            }
        }

    } catch (_: Exception) {

        emptyList()
    }
}
