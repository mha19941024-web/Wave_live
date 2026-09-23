package com.vyro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

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

private enum class WaveTab {
    HOME,
    LIVE,
    CREATE,
    INBOX,
    PROFILE
}

@Composable
private fun WaveRoot() {

    val context = LocalContext.current

    var loggedIn by remember {
        mutableStateOf(
            WaveApi.isLoggedIn(context)
        )
    }

    var loading by remember {
        mutableStateOf(
            loggedIn
        )
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            val session =
                WaveApi.createSession(context)

            if (session.isFailure) {
                WaveApi.clearSession(context)
                loggedIn = false
            }

            loading = false
        } else {
            loading = false
        }
    }

    if (loading) {
        LoadingScreen()
        return
    }

    if (!loggedIn) {
        AuthScreen(
            onLoggedIn = {
                loggedIn = true
            }
        )
        return
    }

    WaveApp(
        onLoggedOut = {
            WaveApi.clearSession(context)
            loggedIn = false
        }
    )
}

@Composable
private fun LoadingScreen() {

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
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            CircularProgressIndicator()

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "جاري تحميل Wave Live...",
                color = Color.White
            )
        }
    }
}

@Composable
private fun AuthScreen(
    onLoggedIn: () -> Unit
) {

    val context = LocalContext.current

    var registerMode by remember {
        mutableStateOf(false)
    }

    var username by remember {
        mutableStateOf("")
    }

    var displayName by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "WAVE",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Wave Live",
                color = Color.LightGray,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Text(
                text = if (registerMode) {
                    "إنشاء حساب جديد"
                } else {
                    "تسجيل الدخول"
                },
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("اسم المستخدم")
                },
                singleLine = true
            )

            if (registerMode) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("الاسم الظاهر")
                    },
                    singleLine = true
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("كلمة المرور")
                },
                visualTransformation =
                    PasswordVisualTransformation(),
                singleLine = true
            )

            if (error.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Button(
                onClick = {

                    if (loading) {
                        return@Button
                    }

                    loading = true
                    error = ""

                    kotlinx.coroutines.MainScope().launch {

                        val result =
                            if (registerMode) {
                                WaveApi.register(
                                    context = context,
                                    username = username,
                                    password = password,
                                    displayName = displayName
                                )
                            } else {
                                WaveApi.login(
                                    context = context,
                                    username = username,
                                    password = password
                                )
                            }

                        loading = false

                        if (result.isSuccess) {
                            onLoggedIn()
                        } else {
                            error =
                                result.exceptionOrNull()
                                    ?.message
                                    ?: "حدث خطأ"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {

                Text(
                    text = if (loading) {
                        "جاري التنفيذ..."
                    } else if (registerMode) {
                        "إنشاء الحساب"
                    } else {
                        "دخول"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            TextButton(
                onClick = {
                    registerMode = !registerMode
                    error = ""
                }
            ) {

                Text(
                    text = if (registerMode) {
                        "لديك حساب؟ تسجيل الدخول"
                    } else {
                        "ليس لديك حساب؟ إنشاء حساب"
                    }
                )
            }
        }
    }
}

@Composable
private fun WaveApp(
    onLoggedOut: () -> Unit
) {

    var selectedTab by remember {
        mutableStateOf(WaveTab.HOME)
    }

    var selectedLiveId by remember {
        mutableStateOf<String?>(null)
    }

    if (selectedLiveId != null) {

        LiveRoomScreen(
            liveId = selectedLiveId!!,
            onBack = {
                selectedLiveId = null
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Box(
            modifier = Modifier
                .weight(1f, fill = true)
        ) {

            when (selectedTab) {

                WaveTab.HOME -> {
                    HomeScreen()
                }

                WaveTab.LIVE -> {
                    LiveListScreen(
                        onOpenLive = {
                            selectedLiveId = it
                        }
                    )
                }

                WaveTab.CREATE -> {
                    CreateScreen()
                }

                WaveTab.INBOX -> {
                    InboxScreen()
                }

                WaveTab.PROFILE -> {
                    ProfileScreen(
                        onLoggedOut = onLoggedOut
                    )
                }
            }
        }

        BottomNavigation(
            selected = selectedTab,
            onSelected = {
                selectedTab = it
            }
        )
    }
}

@Composable
private fun BottomNavigation(
    selected: WaveTab,
    onSelected: (WaveTab) -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xFF111111)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 8.dp,
                    horizontal = 4.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            BottomItem(
                title = "الرئيسية",
                selected = selected == WaveTab.HOME,
                onClick = {
                    onSelected(WaveTab.HOME)
                }
            )

            BottomItem(
                title = "LIVE",
                selected = selected == WaveTab.LIVE,
                onClick = {
                    onSelected(WaveTab.LIVE)
                }
            )

            BottomItem(
                title = "إنشاء",
                selected = selected == WaveTab.CREATE,
                onClick = {
                    onSelected(WaveTab.CREATE)
                }
            )

            BottomItem(
                title = "الوارد",
                selected = selected == WaveTab.INBOX,
                onClick = {
                    onSelected(WaveTab.INBOX)
                }
            )

            BottomItem(
                title = "حسابي",
                selected = selected == WaveTab.PROFILE,
                onClick = {
                    onSelected(WaveTab.PROFILE)
                }
            )
        }
    }
}

@Composable
private fun BottomItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            color =
                if (selected) {
                    Color.White
                } else {
                    Color.Gray
                },
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}

@Composable
private fun HomeScreen() {

    val context = LocalContext.current

    var videos by remember {
        mutableStateOf(
            emptyList<WaveApi.Video>()
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        loading = true

        val result =
            WaveApi.feed(context)

        loading = false

        if (result.isSuccess) {
            videos =
                result.getOrDefault(
                    emptyList()
                )
            error = ""
        } else {
            error =
                result.exceptionOrNull()
                    ?.message
                    ?: "تعذر تحميل الفيديوهات"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Text(
            text = "Wave Live",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 18.dp,
                bottom = 10.dp
            )
        )

        if (loading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

            return
        }

        if (error.isNotBlank() && videos.isEmpty()) {

            EmptyState(
                title = "تعذر تحميل المحتوى",
                message = error
            )

            return
        }

        if (videos.isEmpty()) {

            EmptyState(
                title = "لا توجد فيديوهات الآن",
                message = "ابدأ بنشر أول فيديو في Wave Live."
            )

            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            items(
                items = videos,
                key = {
                    it.id
                }
            ) { video ->

                VideoCard(
                    video = video
                )
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: WaveApi.Video
) {

    val context = LocalContext.current

    var liked by remember {
        mutableStateOf(video.liked)
    }

    var likes by remember {
        mutableIntStateOf(video.likes)
    }

    val player = remember(video.videoUrl) {

        ExoPlayer.Builder(context)
            .build()
            .apply {

                if (video.videoUrl.isNotBlank()) {

                    setMediaItem(
                        MediaItem.fromUri(
                            video.videoUrl
                        )
                    )

                    prepare()
                    playWhenReady = false
                }
            }
    }

    DisposableEffect(player) {

        onDispose {
            player.release()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor =
                Color(0xFF151515)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            if (video.videoUrl.isNotBlank()) {

                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            Color(0xFF222222)
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "الفيديو غير متاح",
                        color = Color.LightGray
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text =
                        if (video.displayName.isNotBlank()) {
                            video.displayName
                        } else {
                            "@${video.username}"
                        },
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (video.caption.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = video.caption,
                        color = Color.White
                    )
                }

                if (!video.musicName.isNullOrBlank()) {

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "Music: ${video.musicName}",
                        color = Color.LightGray
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    TextButton(
                        onClick = {

                            kotlinx.coroutines.MainScope()
                                .launch {

                                    val result =
                                        WaveApi.likeVideo(
                                            context,
                                            video.id
                                        )

                                    if (result.isSuccess) {

                                        liked = !liked

                                        likes =
                                            if (liked) {
                                                likes + 1
                                            } else {
                                                (likes - 1)
                                                    .coerceAtLeast(0)
                                            }
                                    }
                                }
                        }
                    ) {

                        Text(
                            text =
                                if (liked) {
                                    "أعجبني ✓ $likes"
                                } else {
                                    "إعجاب $likes"
                                }
                        )
                    }

                    Text(
                        text = "تعليقات ${video.comments}",
                        color = Color.LightGray,
                        modifier = Modifier.padding(
                            top = 12.dp
                        )
                    )

                    Text(
                        text = "مشاهدات ${video.views}",
                        color = Color.LightGray,
                        modifier = Modifier.padding(
                            top = 12.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveListScreen(
    onOpenLive: (String) -> Unit
) {

    val context = LocalContext.current

    var lives by remember {
        mutableStateOf(
            emptyList<WaveApi.Live>()
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        val result =
            WaveApi.getLives(context)

        loading = false

        if (result.isSuccess) {

            lives =
                result.getOrDefault(
                    emptyList()
                )

        } else {

            error =
                result.exceptionOrNull()
                    ?.message
                    ?: "تعذر تحميل البثوث"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Text(
            text = "البث المباشر",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                16.dp
            )
        )

        if (loading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

            return
        }

        if (error.isNotBlank() && lives.isEmpty()) {

            EmptyState(
                title = "تعذر تحميل البث",
                message = error
            )

            return
        }

        if (lives.isEmpty()) {

            EmptyState(
                title = "لا يوجد بث مباشر الآن",
                message = "يمكنك بدء بث جديد من تبويب إنشاء."
            )

            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            items(
                items = lives,
                key = {
                    it.id
                }
            ) { live ->

                LiveCard(
                    live = live,
                    onClick = {
                        onOpenLive(live.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveCard(
    live: WaveApi.Live,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp
            )
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF181818)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "LIVE",
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    live.title.ifBlank {
                        "بث مباشر"
                    },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    if (live.displayName.isNotBlank()) {
                        live.displayName
                    } else {
                        "@${live.username}"
                    },
                color = Color.LightGray
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "المشاهدون: ${live.viewerCount}",
                color = Color.White
            )
        }
    }
}

@Composable
private fun LiveRoomScreen(
    liveId: String,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    var live by remember {
        mutableStateOf<WaveApi.Live?>(null)
    }

    var gifts by remember {
        mutableStateOf(
            emptyList<WaveApi.Gift>()
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var message by remember {
        mutableStateOf("")
    }

    LaunchedEffect(liveId) {

        loading = true

        val liveResult =
            WaveApi.getLive(
                context,
                liveId
            )

        live =
            liveResult.getOrNull()

        val giftsResult =
            WaveApi.getGifts(context)

        gifts =
            giftsResult.getOrDefault(
                emptyList()
            )

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {

                Text(
                    text = "رجوع"
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "البث المباشر",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (loading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

            return
        }

        val currentLive = live

        if (currentLive == null) {

            EmptyState(
                title = "البث غير متاح",
                message = "تعذر فتح غرفة البث."
            )

            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            item {

                Text(
                    text =
                        currentLive.title.ifBlank {
                            "بث مباشر"
                        },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                )

                Text(
                    text =
                        currentLive.displayName,
                    color = Color.LightGray,
                    modifier = Modifier.padding(
                        horizontal = 16.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (
                    !currentLive.playbackUrl
                        .isNullOrBlank()
                ) {

                    LivePlayer(
                        url = currentLive.playbackUrl!!
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(
                                Color(0xFF151515)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "البث تم إنشاؤه",
                                color = Color.White
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    "في انتظار رابط التشغيل",
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text =
                        "المشاهدون: ${currentLive.viewerCount}",
                    color = Color.White,
                    modifier = Modifier.padding(
                        horizontal = 16.dp
                    )
                )

                if (message.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = message,
                        color = Color(0xFF8BE28B),
                        modifier = Modifier.padding(
                            horizontal = 16.dp
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = "الهدايا",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 16.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            items(
                items = gifts,
                key = {
                    it.id
                }
            ) { gift ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF181818)
                        )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                gift.icon.ifBlank {
                                    "🎁"
                                },
                            fontSize = MaterialTheme
                                .typography
                                .headlineSmall
                                .fontSize
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Column(
                            modifier = Modifier.weight(
                                1f
                            )
                        ) {

                            Text(
                                text = gift.name,
                                color = Color.White,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "${gift.price} Wave Coins",
                                color = Color.LightGray
                            )
                        }

                        Button(
                            onClick = {

                                kotlinx.coroutines
                                    .MainScope()
                                    .launch {

                                        val result =
                                            WaveApi.sendGift(
                                                context =
                                                    context,
                                                liveId =
                                                    currentLive.id,
                                                giftId =
                                                    gift.id,
                                                quantity = 1
                                            )

                                        message =
                                            if (
                                                result.isSuccess
                                            ) {
                                                "تم إرسال ${gift.name}"
                                            } else {
                                                result
                                                    .exceptionOrNull()
                                                    ?.message
                                                    ?: "تعذر إرسال الهدية"
                                            }
                                    }
                            }
                        ) {

                            Text("إرسال")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePlayer(
    url: String
) {

    val context = LocalContext.current

    val player = remember(url) {

        ExoPlayer.Builder(context)
            .build()
            .apply {

                setMediaItem(
                    MediaItem.fromUri(url)
                )

                prepare()
                playWhenReady = true
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
                this.player = player
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
private fun CreateScreen() {

    val context = LocalContext.current

    var title by remember {
        mutableStateOf("")
    }

    var creating by remember {
        mutableStateOf(false)
    }

    var createdLive by remember {
        mutableStateOf<WaveApi.Live?>(null)
    }

    var message by remember {
        mutableStateOf("")
    }

    var music by remember {
        mutableStateOf(
            emptyList<WaveApi.MusicTrack>()
        )
    }

    var effects by remember {
        mutableStateOf(
            emptyList<WaveApi.VisualEffect>()
        )
    }

    LaunchedEffect(Unit) {

        music =
            WaveApi.getMusic(context)
                .getOrDefault(emptyList())

        effects =
            WaveApi.getEffects(context)
                .getOrDefault(emptyList())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        Text(
            text = "إنشاء محتوى",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            text = "بدء بث مباشر",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                message = ""
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("عنوان البث")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Button(
            onClick = {

                if (creating) {
                    return@Button
                }

                creating = true
                message = ""

                kotlinx.coroutines
                    .MainScope()
                    .launch {

                        val result =
                            WaveApi.createLive(
                                context,
                                title
                            )

                        creating = false

                        if (result.isSuccess) {

                            createdLive =
                                result.getOrNull()

                            message =
                                "تم إنشاء غرفة البث بنجاح"

                        } else {

                            message =
                                result
                                    .exceptionOrNull()
                                    ?.message
                                    ?: "تعذر إنشاء البث"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                !creating &&
                    title.trim().isNotBlank()
        ) {

            Text(
                text =
                    if (creating) {
                        "جاري الإنشاء..."
                    } else {
                        "بدء البث"
                    }
            )
        }

        if (message.isNotBlank()) {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = message,
                color = Color.White
            )
        }

        createdLive?.let { live ->

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF181818)
                    )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "بيانات البث",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    InfoLine(
                        title = "Live ID",
                        value = live.id
                    )

                    InfoLine(
                        title = "RTMPS",
                        value =
                            live.rtmpsUrl
                                ?: "غير متاح بعد"
                    )

                    InfoLine(
                        title = "Stream Key",
                        value =
                            live.streamKey
                                ?: "غير متاح بعد"
                    )

                    InfoLine(
                        title = "Playback",
                        value =
                            live.playbackUrl
                                ?: "غير متاح بعد"
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "مكتبة الموسيقى",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (music.isEmpty()) {

            Text(
                text = "لا توجد موسيقى متاحة حاليًا.",
                color = Color.Gray
            )

        } else {

            music.take(10).forEach { track ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 4.dp
                        ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF171717)
                        )
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = track.title,
                            color = Color.White,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text = track.artist,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "المؤثرات البصرية",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (effects.isEmpty()) {

            Text(
                text = "لا توجد مؤثرات متاحة حاليًا.",
                color = Color.Gray
            )

        } else {

            effects.take(10).forEach { effect ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 4.dp
                        ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF171717)
                        )
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = effect.name,
                            color = Color.White,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "${effect.type} • ${effect.value}",
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun InboxScreen() {

    val context = LocalContext.current

    var history by remember {
        mutableStateOf(
            emptyList<org.json.JSONObject>()
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        val result =
            WaveApi.getGiftHistory(context)

        history =
            result.getOrDefault(
                emptyList()
            )

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Text(
            text = "الوارد",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (loading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

            return
        }

        if (history.isEmpty()) {

            EmptyState(
                title = "لا توجد معاملات هدايا",
                message = "ستظهر هنا الهدايا والمعاملات الخاصة بحسابك."
            )

            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            itemsIndexed(
                history
            ) { _, item ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF171717)
                        )
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Text(
                            text =
                                item.optString(
                                    "giftName",
                                    "هدية"
                                ),
                            color = Color.White,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                item.optString(
                                    "totalCoins",
                                    "0"
                                ) + " Coins",
                            color = Color.LightGray
                        )

                        Text(
                            text =
                                item.optString(
                                    "createdAt",
                                    ""
                                ),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    onLoggedOut: () -> Unit
) {

    val context = LocalContext.current

    var user by remember {
        mutableStateOf<WaveApi.User?>(null)
    }

    var wallet by remember {
        mutableStateOf<WaveApi.Wallet?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        loading = true

        val userResult =
            WaveApi.me(context)

        if (userResult.isSuccess) {

            user =
                userResult.getOrNull()

        } else {

            error =
                userResult
                    .exceptionOrNull()
                    ?.message
                    ?: ""
        }

        wallet =
            WaveApi.getWallet(context)
                .getOrNull()

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        Text(
            text = "حسابي",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        if (loading) {

            CircularProgressIndicator()

            return
        }

        if (user == null) {

            EmptyState(
                title = "تعذر تحميل الحساب",
                message = error.ifBlank {
                    "حاول تسجيل الدخول مرة أخرى."
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onLoggedOut,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("تسجيل الدخول من جديد")
            }

            return
        }

        val currentUser =
            user!!

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF181818)
                )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text =
                        currentUser.displayName
                            .ifBlank {
                                currentUser.username
                            },
                    color = Color.White,
                    style =
                        MaterialTheme.typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "@${currentUser.username}",
                    color = Color.LightGray
                )

                if (!currentUser.bio.isNullOrBlank()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = currentUser.bio!!,
                        color = Color.White
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    StatBox(
                        title = "المتابعون",
                        value =
                            currentUser.followers
                                .toString()
                    )

                    StatBox(
                        title = "المتابَعون",
                        value =
                            currentUser.following
                                .toString()
                    )

                    StatBox(
                        title = "Coins",
                        value =
                            currentUser.coins
                                .toString()
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        wallet?.let { currentWallet ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF181818)
                    )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "محفظة Wave",
                        color = Color.White,
                        style =
                            MaterialTheme.typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "${currentWallet.coins} Wave Coins",
                        color = Color.White,
                        style =
                            MaterialTheme.typography
                                .headlineSmall
                    )

                    if (
                        currentWallet
                            .paymentMethods
                            .isNotEmpty()
                    ) {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "طرق الدفع: " +
                                    currentWallet
                                        .paymentMethods
                                        .joinToString(
                                            " • "
                                        ),
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = {

                kotlinx.coroutines
                    .MainScope()
                    .launch {

                        WaveApi.logout(
                            context
                        )

                        onLoggedOut()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("تسجيل الخروج")
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = title,
            color = Color.Gray
        )
    }
}

@Composable
private fun InfoLine(
    title: String,
    value: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 6.dp
            )
    ) {

        Text(
            text = title,
            color = Color.Gray
        )

        Text(
            text = value,
            color = Color.White
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                color = Color.White,
                style =
                    MaterialTheme.typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = message,
                color = Color.Gray,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}
