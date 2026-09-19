package com.vyro.app

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
                URL(API_BASE + path).openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
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

            val code = connection.responseCode

            val input =
                if (code in 200..399) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response =
                input?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

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

    suspend fun createVideo(
        token: String,
        url: String,
        caption: String
    ): ApiResult {

        val body =
            JSONObject()
                .put("url", url)
                .put("caption", caption)
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

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
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

        if (session.code !in 200..299) {

            errorMessage =
                "تعذر الاتصال بخادم Wave Live"

            loading = false

            return@LaunchedEffect
        }

        try {

            val sessionJson =
                JSONObject(session.body)

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

            if (token.isNotBlank()) {

                val meResult =
                    WaveApi.me(token)

                if (meResult.code in 200..299) {

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

                if (feedResult.code in 200..299) {

                    val feedJson =
                        JSONObject(
                            feedResult.body
                        )

                    val array =
                        feedJson.optJSONArray(
                            "videos"
                        ) ?: feedJson.optJSONArray(
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
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WaveDark)
                .padding(innerPadding)
        ) {

            if (loading) {

                LoadingScreen()

            } else {

                when (selectedTab) {

                    0 -> {

                        HomeScreen(
                            videos = videos,
                            onLike = { video ->

                                val currentToken =
                                    token

                                val newLiked =
                                    !video.liked

                                videos =
                                    videos.map { item ->

                                        if (
                                            item.id ==
                                            video.id
                                        ) {

                                            item.copy(
                                                liked =
                                                    newLiked,
                                                likes =
                                                    if (
                                                        newLiked
                                                    ) {
                                                        item.likes + 1
                                                    } else {
                                                        maxOf(
                                                            0,
                                                            item.likes - 1
                                                        )
                                                    }
                                            )

                                        } else {

                                            item
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
                    }

                    1 -> {

                        LiveScreen(
                            token = token
                        )
                    }

                    2 -> {

                        CreateScreen(
                            token = token,
                            onPublished = {

                                scope.launch {

                                    val result =
                                        WaveApi.feed(
                                            token
                                        )

                                    if (
                                        result.code in
                                        200..299
                                    ) {

                                        try {

                                            val json =
                                                JSONObject(
                                                    result.body
                                                )

                                            val array =
                                                json.optJSONArray(
                                                    "videos"
                                                ) ?: json.optJSONArray(
                                                    "data"
                                                )

                                            videos =
                                                parseVideos(
                                                    array
                                                )

                                        } catch (
                                            _: Exception
                                        ) {
                                        }
                                    }
                                }
                            }
                        )
                    }

                    3 -> {

                        InboxScreen()
                    }

                    4 -> {

                        ProfileScreen(
                            token = token,
                            user = user,
                            onUserChanged = {
                                user = it
                            }
                        )
                    }
                }
            }

            if (errorMessage.isNotBlank()) {

                Card(
                    modifier = Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(12.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF3A1725)
                        )
                ) {

                    Text(
                        text = errorMessage,
                        color = Color.White,
                        modifier =
                            Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {

    Column(
        modifier =
            Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "WAVE",
            color = WavePurple,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
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
            .padding(
                vertical = 8.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceAround,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        BottomItem(
            icon = Icons.Default.Home,
            text = "الرئيسية",
            selected = selected == 0,
            onClick = {
                onSelected(0)
            }
        )

        BottomItem(
            icon = Icons.Default.Videocam,
            text = "LIVE",
            selected = selected == 1,
            onClick = {
                onSelected(1)
            }
        )

        BottomItem(
            icon = Icons.Default.Add,
            text = "إنشاء",
            selected = selected == 2,
            onClick = {
                onSelected(2)
            }
        )

        BottomItem(
            icon = Icons.Default.Chat,
            text = "الوارد",
            selected = selected == 3,
            onClick = {
                onSelected(3)
            }
        )

        BottomItem(
            icon = Icons.Default.Person,
            text = "حسابي",
            selected = selected == 4,
            onClick = {
                onSelected(4)
            }
        )
    }
}

@Composable
private fun BottomItem(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 8.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "WAVE",
                color = WavePurple,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    "لا توجد فيديوهات منشورة حتى الآن",
                color = Color.White
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "ابدأ بنشر أول فيديو في Wave Live",
                color = Color.LightGray
            )
        }

        return
    }

    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
    ) {

        item {

            Text(
                text = "WAVE",
                color = WavePurple,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    )
            )
        }

        items(
            items = videos,
            key = {
                it.id
            }
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

    val context =
        LocalContext.current

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                ),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = WaveCard
            )
    ) {

        Column {

            if (video.url.isNotBlank()) {

                AndroidView(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                    factory = {

                        VideoView(context).apply {

                            setVideoURI(
                                Uri.parse(
                                    video.url
                                )
                            )

                            val controller =
                                MediaController(
                                    context
                                )

                            controller.setAnchorView(
                                this
                            )

                            setMediaController(
                                controller
                            )

                            setOnPreparedListener {
                                it.isLooping = true
                                start()
                            }
                        }
                    }
                )

            } else {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                Color.Black
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "WAVE",
                        color = WavePurple,
                        fontSize = 38.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            if (
                                video.user.isBlank()
                            ) {
                                "@wave_user"
                            } else {
                                "@${video.user}"
                            },
                        color = Color.White,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = video.caption,
                        color =
                            Color.LightGray
                    )
                }

                IconButton(
                    onClick = onLike
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Favorite,
                        contentDescription =
                            "إعجاب",
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
                    color = Color.White
                )

                IconButton(
                    onClick = {

                        val intent =
                            Intent(
                                Intent.ACTION_SEND
                            )

                        intent.type =
                            "text/plain"

                        intent.putExtra(
                            Intent.EXTRA_TEXT,
                            "شاهد هذا الفيديو على Wave Live"
                        )

                        context.startActivity(
                            Intent.createChooser(
                                intent,
                                "مشاركة"
                            )
                        )
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Send,
                        contentDescription =
                            "مشاركة",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveScreen(
    token: String
) {

    var title by remember {
        mutableStateOf("")
    }

    var status by remember {
        mutableStateOf("")
    }

    var starting by remember {
        mutableStateOf(false)
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
            text = "Wave LIVE",
            color = WavePurple,
            fontSize = 32.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
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
            modifier =
                Modifier.height(20.dp)
        )

        Button(
            onClick = {

                if (title.isBlank()) {

                    status =
                        "اكتب عنوان البث أولاً"

                    return@Button
                }

                starting = true

                scope.launch {

                    val result =
                        WaveApi.createLive(
                            token,
                            title
                        )

                    starting = false

                    status =
                        if (
                            result.code in
                            200..299
                        ) {
                            "تم إنشاء البث بنجاح"
                        } else {
                            "تعذر إنشاء البث (${result.code})"
                        }
                }
            },
            enabled = !starting,
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        WavePurple
                )
        ) {

            Icon(
                imageVector =
                    Icons.Default.Videocam,
                contentDescription = null
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                text =
                    if (starting) {
                        "جارٍ إنشاء البث..."
                    } else {
                        "بدء بث مباشر"
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = status,
            color = Color.White
        )
    }
}

@Composable
private fun CreateScreen(
    token: String,
    onPublished: () -> Unit
) {

    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var caption by remember {
        mutableStateOf("")
    }

    var status by remember {
        mutableStateOf("")
    }

    var publishing by remember {
        mutableStateOf(false)
    }

    val scope =
        rememberCoroutineScope()

    val picker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            selectedUri = uri

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
                .padding(20.dp)
    ) {

        Text(
            text = "إنشاء فيديو",
            color = WavePurple,
            fontSize = 30.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Button(
            onClick = {
                picker.launch("video/*")
            },
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        WavePurple
                )
        ) {

            Icon(
                imageVector =
                    Icons.Default.Add,
                contentDescription = null
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                text =
                    "اختيار فيديو من الهاتف"
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        selectedUri?.let { uri ->

            AndroidView(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                factory = { context ->

                    VideoView(context).apply {

                        setVideoURI(uri)

                        setOnPreparedListener {
                            it.isLooping = true
                            start()
                        }
                    }
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
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
            modifier =
                Modifier.height(16.dp)
        )

        Button(
            onClick = {

                val uri =
                    selectedUri

                if (uri == null) {

                    status =
                        "اختر فيديو أولاً"

                    return@Button
                }

                publishing = true

                scope.launch {

                    val result =
                        WaveApi.createVideo(
                            token = token,
                            url = uri.toString(),
                            caption = caption
                        )

                    publishing = false

                    if (
                        result.code in
                        200..299
                    ) {

                        status =
                            "تم نشر الفيديو"

                        onPublished()

                    } else {

                        status =
                            "تعذر نشر الفيديو (${result.code})"
                    }
                }
            },
            enabled = !publishing,
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        WavePurple
                )
        ) {

            Text(
                text =
                    if (publishing) {
                        "جارٍ النشر..."
                    } else {
                        "نشر الفيديو"
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text = status,
            color = Color.White
        )
    }
}

@Composable
private fun InboxScreen() {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Icon(
            imageVector =
                Icons.Default.Chat,
            contentDescription = null,
            tint = WavePurple,
            modifier =
                Modifier.size(70.dp)
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text = "الوارد",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "الإشعارات والرسائل ستظهر هنا",
            color = Color.LightGray
        )
    }
}

@Composable
private fun ProfileScreen(
    token: String,
    user: WaveUser?,
    onUserChanged: (WaveUser) -> Unit
) {

    var displayName by remember(
        user?.displayName
    ) {
        mutableStateOf(
            user?.displayName ?: ""
        )
    }

    var bio by remember(
        user?.bio
    ) {
        mutableStateOf(
            user?.bio ?: ""
        )
    }

    var status by remember {
        mutableStateOf("")
    }

    var walletVisible by remember {
        mutableStateOf(false)
    }

    val scope =
        rememberCoroutineScope()

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
                    )
                    .align(
                        Alignment.CenterHorizontally
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier =
                    Modifier.size(55.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                if (
                    user?.username.isNullOrBlank()
                ) {
                    "@wave_user"
                } else {
                    "@${user?.username}"
                },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
            },
            label = {
                Text("اسم العرض")
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = bio,
            onValueChange = {
                bio = it
            },
            label = {
                Text("نبذة عنك")
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Button(
            onClick = {

                scope.launch {

                    val result =
                        WaveApi.updateProfile(
                            token = token,
                            displayName =
                                displayName,
                            bio = bio
                        )

                    if (
                        result.code in
                        200..299
                    ) {

                        val updated =
                            (user ?: WaveUser()).copy(
                                displayName =
                                    displayName,
                                bio = bio
                            )

                        onUserChanged(
                            updated
                        )

                        status =
                            "تم حفظ الملف الشخصي"

                    } else {

                        status =
                            "تعذر حفظ الملف الشخصي"
                    }
                }
            },
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        WavePurple
                )
        ) {

            Text(
                text =
                    "حفظ التغييرات"
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Button(
            onClick = {
                walletVisible = true
            },
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF24242D)
                )
        ) {

            Icon(
                imageVector =
                    Icons.Default.Wallet,
                contentDescription = null
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                text =
                    "محفظة Wave"
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text = status,
            color = Color.White
        )
    }

    if (walletVisible) {

        WalletDialog(
            onClose = {
                walletVisible = false
            }
        )
    }
}

@Composable
private fun WalletDialog(
    onClose: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = "محفظة Wave"
            )
        },
        text = {

            Column {

                Text(
                    text =
                        "طرق شحن المحفظة"
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        "المحفظة الأولى: 01284306120"
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "المحفظة الثانية: 01144210918"
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text = "Wave Coins",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "رصيد العملات سيظهر بعد ربط عملية الشحن بالخادم."
                )
            }
        },
        confirmButton = {

            TextButton(
                onClick = onClose
            ) {

                Text(
                    text = "إغلاق"
                )
            }
        }
    )
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
            json.optString("displayName"),
        bio =
            json.optString("bio"),
        avatarUrl =
            json.optString("avatarUrl")
    )
}

private fun parseVideos(
    array: JSONArray?
): List<WaveVideo> {

    if (array == null) {
        return emptyList()
    }

    val result =
        mutableListOf<WaveVideo>()

    for (
        index in
        0 until array.length()
    ) {

        val item =
            array.optJSONObject(index)
                ?: continue

        result += WaveVideo(
            id =
                item.optString("id"),
            url =
                item.optString("url"),
            user =
                item.optString("user"),
            caption =
                item.optString("caption"),
            likes =
                item.optInt("likes", 0),
            liked =
                item.optBoolean(
                    "liked",
                    false
                )
        )
    }

    return result
}
