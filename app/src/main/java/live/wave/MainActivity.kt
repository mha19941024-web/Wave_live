package com.vyro.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val PREFS = "vyro_session"
private const val API = BuildConfig.API_BASE_URL

data class Video(
    val id: String,
    val url: String,
    val user: String,
    val caption: String,
    val likes: Int
)

data class Me(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String
)

data class UploadTicket(
    val uploadUrl: String,
    val id: String
)

data class LiveResult(
    val rtmps: String,
    val key: String
)

enum class Tab {
    HOME,
    LIVE,
    CREATE,
    INBOX,
    PROFILE
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaveRoot()
        }
    }
}

@Composable
fun WaveRoot() {

    var started by rememberSaveable {
        mutableStateOf(false)
    }

    if (!started) {
        SplashScreen(
            onStart = {
                started = true
            }
        )
    } else {
        WaveApp(initialTab = Tab.LIVE)
    }
}

@Composable
fun SplashScreen(
    onStart: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "WAVE",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Wave Live",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(52.dp)
            )

            Button(
                onClick = onStart,
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6F4BB8)
                )
            ) {
                Text(
                    text = "Start Live",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun WaveApp(
    initialTab: Tab = Tab.HOME
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var token by remember {
        mutableStateOf(
            context
                .getSharedPreferences(PREFS, 0)
                .getString("token", null)
        )
    }

    var tab by rememberSaveable {
        mutableStateOf(initialTab)
    }

    var videos by remember {
        mutableStateOf<List<Video>>(emptyList())
    }

    var me by remember {
        mutableStateOf<Me?>(null)
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    suspend fun ensureSession() {

        if (token == null) {

            val newToken = Api.session()

            token = newToken

            context
                .getSharedPreferences(PREFS, 0)
                .edit()
                .putString("token", newToken)
                .apply()
        }
    }

    fun load() {

        scope.launch {

            busy = true
            message = null

            try {

                ensureSession()

                val currentToken = token
                    ?: throw IllegalStateException("Session unavailable")

                videos = Api.feed(currentToken)
                me = Api.me(currentToken)

            } catch (e: Exception) {

                message = e.message ?: "Connection error"

            } finally {

                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        load()
    }

    Scaffold(
        containerColor = Color.Black,

        bottomBar = {

            NavigationBar(
                containerColor = Color.Black
            ) {

                Nav(
                    tab = Tab.HOME,
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = tab,
                    onSelect = {
                        tab = it
                    }
                )

                Nav(
                    tab = Tab.LIVE,
                    icon = Icons.Default.LiveTv,
                    label = "LIVE",
                    selected = tab,
                    onSelect = {
                        tab = it
                    }
                )

                Nav(
                    tab = Tab.CREATE,
                    icon = Icons.Default.AddCircle,
                    label = "Create",
                    selected = tab,
                    onSelect = {
                        tab = it
                    }
                )

                Nav(
                    tab = Tab.INBOX,
                    icon = Icons.Default.Notifications,
                    label = "Inbox",
                    selected = tab,
                    onSelect = {
                        tab = it
                    }
                )

                Nav(
                    tab = Tab.PROFILE,
                    icon = Icons.Default.Person,
                    label = "Profile",
                    selected = tab,
                    onSelect = {
                        tab = it
                    }
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when (tab) {

                Tab.HOME -> {

                    Home(
                        videos = videos,
                        busy = busy,
                        error = message,
                        retry = {
                            load()
                        },
                        onLike = { video, liked ->

                            scope.launch {

                                try {

                                    ensureSession()

                                    val currentToken = token
                                        ?: throw IllegalStateException(
                                            "Session unavailable"
                                        )

                                    Api.like(
                                        token = currentToken,
                                        id = video.id,
                                        liked = liked
                                    )

                                    videos = Api.feed(currentToken)

                                } catch (e: Exception) {

                                    message = e.message
                                }
                            }
                        }
                    )
                }

                Tab.LIVE -> {

                    Live(
                        token = token,
                        setBusy = {
                            busy = it
                        },
                        onMessage = {
                            message = it
                        }
                    )
                }

                Tab.CREATE -> {

                    Create(
                        token = token,
                        setBusy = {
                            busy = it
                        },
                        done = { msg ->

                            message = msg
                            load()
                        }
                    )
                }

                Tab.INBOX -> {

                    Center(
                        text = "INBOX\nالإشعارات قريباً"
                    )
                }

                Tab.PROFILE -> {

                    Profile(
                        me = me,
                        save = { name, bio ->

                            scope.launch {

                                try {

                                    ensureSession()

                                    val currentToken = token
                                        ?: throw IllegalStateException(
                                            "Session unavailable"
                                        )

                                    me = Api.profile(
                                        token = currentToken,
                                        name = name,
                                        bio = bio
                                    )

                                    message = "تم حفظ الملف"

                                } catch (e: Exception) {

                                    message = e.message
                                }
                            }
                        }
                    )
                }
            }

            if (message != null) {

                Text(
                    text = message ?: "",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun Nav(
    tab: Tab,
    icon: ImageVector,
    label: String,
    selected: Tab,
    onSelect: (Tab) -> Unit
) {

    NavigationBarItem(
        selected = tab == selected,
        onClick = {
            onSelect(tab)
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        label = {
            Text(label)
        }
    )
}

@Composable
fun Home(
    videos: List<Video>,
    busy: Boolean,
    error: String?,
    retry: () -> Unit,
    onLike: (Video, Boolean) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        if (error != null) {

            Text(
                text = error,
                color = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }

        when {

            busy && videos.isEmpty() -> {

                Center("VYRO\nLoading…")
            }

            videos.isEmpty() -> {

                Center("لا توجد فيديوهات بعد")
            }

            else -> {

                LazyColumn {

                    items(
                        items = videos,
                        key = {
                            it.id
                        }
                    ) { video ->

                        VideoCard(
                            video = video,
                            onLike = onLike
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    video: Video,
    onLike: (Video, Boolean) -> Unit
) {

    var liked by remember(video.id) {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(620.dp)
        ) {

            Player(video.url)

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(0.78f)
            ) {

                Text(
                    text = "@${video.user}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = video.caption,
                    color = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {

                    liked = !liked
                    onLike(video, liked)
                }
            ) {

                Icon(
                    imageVector = if (liked) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = "Like",
                    tint = Color.White
                )
            }

            Text(
                text = video.likes.toString(),
                color = Color.White
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Icon(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = "Comments",
                tint = Color.White
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White
            )
        }
    }
}

@Composable
fun Player(
    url: String
) {

    val context = LocalContext.current

    val player = remember(url) {

        ExoPlayer
            .Builder(context)
            .build()
            .apply {

                setMediaItem(
                    MediaItem.fromUri(url)
                )

                prepare()

                playWhenReady = false
            }
    }

    DisposableEffect(player) {

        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = true
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun Create(
    token: String?,
    setBusy: (Boolean) -> Unit,
    done: (String) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var caption by remember {
        mutableStateOf("")
    }

    var selected by remember {
        mutableStateOf<Uri?>(null)
    }

    var status by remember {
        mutableStateOf("")
    }

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            selected = uri
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "CREATE",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = caption,
            onValueChange = {
                caption = it
            },
            label = {
                Text("وصف الفيديو")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {
                picker.launch("video/*")
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = if (selected == null) {
                    "اختيار فيديو من الهاتف"
                } else {
                    "تم اختيار الفيديو"
                }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            enabled = selected != null &&
                    !token.isNullOrBlank(),

            onClick = {

                scope.launch {

                    setBusy(true)
                    status = "جاري الرفع…"

                    try {

                        val currentToken = token
                            ?: throw IllegalStateException(
                                "Session unavailable"
                            )

                        val selectedUri = selected
                            ?: throw IllegalStateException(
                                "No video selected"
                            )

                        val ticket =
                            Api.directUpload(currentToken)

                        val videoId =
                            Api.uploadFile(
                                context = context,
                                uri = selectedUri,
                                uploadUrl = ticket.uploadUrl
                            )

                        Api.createVideo(
                            token = currentToken,
                            id = videoId,
                            caption = caption
                        )

                        status = "تم النشر بنجاح"

                        done(status)

                    } catch (e: Exception) {

                        status =
                            e.message ?: "فشل الرفع"

                        done(status)

                    } finally {

                        setBusy(false)
                    }
                }
            },

            modifier = Modifier.fillMaxWidth()
        ) {

            Text("رفع ونشر")
        }

        Text(
            text = status,
            color = Color.White,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun Live(
    token: String?,
    setBusy: (Boolean) -> Unit,
    onMessage: (String) -> Unit
) {

    val scope = rememberCoroutineScope()

    var rtmp by remember {
        mutableStateOf("")
    }

    var key by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "LIVE",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "أنشئ قناة بث من Cloudflare Stream ثم استخدم RTMPS URL وStream Key في برنامج البث.",
            color = Color.LightGray
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            enabled = !token.isNullOrBlank(),

            onClick = {

                scope.launch {

                    setBusy(true)

                    try {

                        val currentToken = token
                            ?: throw IllegalStateException(
                                "Session unavailable"
                            )

                        val result =
                            Api.live(currentToken)

                        rtmp = result.rtmps
                        key = result.key

                        onMessage(
                            "تم إنشاء قناة البث"
                        )

                    } catch (e: Exception) {

                        onMessage(
                            e.message ?: "فشل إنشاء البث"
                        )

                    } finally {

                        setBusy(false)
                    }
                }
            }
        ) {

            Text("إنشاء بث مباشر")
        }

        if (rtmp.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "RTMPS URL",
                color = Color.Gray
            )

            Text(
                text = rtmp,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "STREAM KEY",
                color = Color.Gray
            )

            Text(
                text = key,
                color = Color.White
            )
        }
    }
}

@Composable
fun Profile(
    me: Me?,
    save: (String, String) -> Unit
) {

    var name by remember(me?.displayName) {
        mutableStateOf(
            me?.displayName.orEmpty()
        )
    }

    var bio by remember(me?.bio) {
        mutableStateOf(
            me?.bio.orEmpty()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "PROFILE",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "@${me?.username ?: "guest"}",
            color = Color.LightGray
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            label = {
                Text("الاسم")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = bio,
            onValueChange = {
                bio = it
            },
            label = {
                Text("نبذة")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {
                save(name, bio)
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("حفظ")
        }
    }
}

@Composable
fun Center(
    text: String
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

object Api {

    private fun conn(
        path: String,
        method: String,
        token: String? = null
    ): HttpURLConnection {

        return (
            URL(API + path)
                .openConnection() as HttpURLConnection
            ).apply {

            requestMethod = method

            connectTimeout = 15000
            readTimeout = 30000

            if (token != null) {
                setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (
                method == "POST" ||
                method == "PATCH"
            ) {

                doOutput = true

                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
            }
        }
    }

    private fun body(
        connection: HttpURLConnection
    ): String {

        val code = connection.responseCode

        val stream =
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val text =
            stream
                ?.bufferedReader()
                ?.use {
                    it.readText()
                }
                .orEmpty()

        if (code !in 200..299) {

            throw IllegalStateException(
                try {
                    JSONObject(text)
                        .optString(
                            "error",
                            "HTTP $code"
                        )
                } catch (_: Exception) {
                    "HTTP $code"
                }
            )
        }

        return text
    }

    suspend fun session(): String =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/session",
                    method = "POST"
                )

            try {

                JSONObject(
                    body(connection)
                ).getString("token")

            } finally {

                connection.disconnect()
            }
        }

    suspend fun feed(
        token: String
    ): List<Video> =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/feed?limit=30",
                    method = "GET",
                    token = token
                )

            try {

                val array =
                    JSONObject(
                        body(connection)
                    ).optJSONArray("items")
                        ?: JSONArray()

                List(array.length()) { index ->

                    val item =
                        array.getJSONObject(index)

                    Video(
                        id = item.getString("id"),
                        url = item.getString("url"),
                        user = item.getString("user"),
                        caption = item.optString("caption"),
                        likes = item.optInt("likes")
                    )
                }

            } finally {

                connection.disconnect()
            }
        }

    suspend fun me(
        token: String
    ): Me =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/me",
                    method = "GET",
                    token = token
                )

            try {

                val user =
                    JSONObject(
                        body(connection)
                    ).getJSONObject("user")

                Me(
                    id = user.getString("id"),
                    username = user.getString("username"),
                    displayName =
                        user.optString("displayName"),
                    bio = user.optString("bio")
                )

            } finally {

                connection.disconnect()
            }
        }

    suspend fun like(
        token: String,
        id: String,
        liked: Boolean
    ) =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/videos/$id/like",
                    method =
                        if (liked) {
                            "POST"
                        } else {
                            "DELETE"
                        },
                    token = token
                )

            try {

                body(connection)

            } finally {

                connection.disconnect()
            }
        }

    suspend fun profile(
        token: String,
        name: String,
        bio: String
    ): Me =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/profile",
                    method = "PATCH",
                    token = token
                )

            try {

                val json =
                    JSONObject()
                        .put(
                            "displayName",
                            name
                        )
                        .put(
                            "bio",
                            bio
                        )

                connection
                    .outputStream
                    .use {
                        it.write(
                            json
                                .toString()
                                .toByteArray()
                        )
                    }

                val user =
                    JSONObject(
                        body(connection)
                    ).getJSONObject("user")

                Me(
                    id = user.getString("id"),
                    username = user.getString("username"),
                    displayName =
                        user.optString("displayName"),
                    bio =
                        user.optString("bio")
                )

            } finally {

                connection.disconnect()
            }
        }

    suspend fun directUpload(
        token: String
    ): UploadTicket =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/upload/direct",
                    method = "POST",
                    token = token
                )

            try {

                val result =
                    JSONObject(
                        body(connection)
                    ).getJSONObject("result")

                UploadTicket(
                    uploadUrl =
                        result.getString(
                            "uploadURL"
                        ),
                    id =
                        result.getString(
                            "uid"
                        )
                )

            } finally {

                connection.disconnect()
            }
        }

    suspend fun createVideo(
        token: String,
        id: String,
        caption: String
    ) =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/videos",
                    method = "POST",
                    token = token
                )

            try {

                val json =
                    JSONObject()
                        .put(
                            "streamId",
                            id
                        )
                        .put(
                            "caption",
                            caption
                        )

                connection
                    .outputStream
                    .use {
                        it.write(
                            json
                                .toString()
                                .toByteArray()
                        )
                    }

                body(connection)

            } finally {

                connection.disconnect()
            }
        }

    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        uploadUrl: String
    ): String =
        withContext(Dispatchers.IO) {

            val boundary =
                "----VYRO${System.currentTimeMillis()}"

            val connection =
                (
                    URL(uploadUrl)
                        .openConnection()
                        as HttpURLConnection
                    ).apply {

                    requestMethod = "POST"

                    doOutput = true

                    connectTimeout = 30000
                    readTimeout = 120000

                    setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=$boundary"
                    )
                }

            try {

                DataOutputStream(
                    connection.outputStream
                ).use { output ->

                    output.writeBytes(
                        "--$boundary\r\n"
                    )

                    output.writeBytes(
                        "Content-Disposition: form-data; " +
                            "name=\"file\"; " +
                            "filename=\"video.mp4\"\r\n"
                    )

                    output.writeBytes(
                        "Content-Type: video/mp4\r\n\r\n"
                    )

                    val input =
                        context.contentResolver
                            .openInputStream(uri)
                            ?: throw IllegalStateException(
                                "Cannot read selected file"
                            )

                    input.use {

                        val buffer =
                            ByteArray(64 * 1024)

                        while (true) {

                            val count =
                                it.read(buffer)

                            if (count <= 0) {
                                break
                            }

                            output.write(
                                buffer,
                                0,
                                count
                            )
                        }
                    }

                    output.writeBytes(
                        "\r\n--$boundary--\r\n"
                    )
                }

                if (connection.responseCode !in 200..299) {

                    throw IllegalStateException(
                        "Upload failed: HTTP ${connection.responseCode}"
                    )
                }

                idFromUploadResponse(
                    connection
                        .inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }
                )

            } finally {

                connection.disconnect()
            }
        }

    private fun idFromUploadResponse(
        text: String
    ): String {

        return JSONObject(text)
            .optJSONObject("result")
            ?.optString("uid")
            ?.takeIf {
                it.isNotBlank()
            }
            ?: throw IllegalStateException(
                "Cloudflare did not return a video id"
            )
    }

    suspend fun live(
        token: String
    ): LiveResult =
        withContext(Dispatchers.IO) {

            val connection =
                conn(
                    path = "/api/live/create",
                    method = "POST",
                    token = token
                )

            try {

                val result =
                    JSONObject(
                        body(connection)
                    ).getJSONObject("result")

                val rtmps =
                    result.optJSONObject("rtmps")
                        ?: throw IllegalStateException(
                            "Cloudflare did not return RTMPS credentials"
                        )

                LiveResult(
                    rtmps =
                        rtmps.optString("url"),
                    key =
                        rtmps.optString("streamKey")
                )
