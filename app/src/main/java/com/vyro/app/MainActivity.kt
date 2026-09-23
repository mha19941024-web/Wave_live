package com.vyro.app

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.VideoView
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

private val WaveBackground = Color(0xFF08060D)
private val WaveSurface = Color(0xFF15111D)
private val WavePurple = Color(0xFF9B5CFF)
private val WavePink = Color(0xFFFF4FA3)
private val WaveGold = Color(0xFFFFC857)
private val WaveGreen = Color(0xFF22C55E)
private val WaveWhite = Color(0xFFF8F5FF)
private val WaveMuted = Color(0xFFA9A1B5)
private val WaveRed = Color(0xFFFF5C67)

private enum class WaveTab {
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
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WaveBackground
                ) {
                    WaveApp()
                }
            }
        }
    }
}

@Composable
private fun WaveApp() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember {
        mutableStateOf(WaveTab.HOME)
    }

    var loggedIn by remember {
        mutableStateOf(WaveApi.isLoggedIn(context))
    }

    var currentUser by remember {
        mutableStateOf<WaveApi.User?>(null)
    }

    var coins by remember {
        mutableIntStateOf(0)
    }

    var showAuth by remember {
        mutableStateOf(!loggedIn)
    }

    LaunchedEffect(loggedIn) {

        if (loggedIn) {

            val result = WaveApi.me(context)

            if (result.isSuccess) {

                currentUser = result.getOrNull()
                coins = result.getOrNull()?.coins ?: 0
                showAuth = false

            } else {

                loggedIn = false
                currentUser = null
                coins = 0
                showAuth = true
            }
        }
    }

    if (showAuth && !loggedIn) {

        AuthScreen(
            onLoginSuccess = { user ->

                loggedIn = true
                currentUser = user
                coins = user?.coins ?: 0
                showAuth = false
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveBackground)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            when (selectedTab) {

                WaveTab.HOME -> {

                    HomeScreen(
                        coins = coins,
                        onOpenLogin = {
                            showAuth = true
                        }
                    )
                }

                WaveTab.LIVE -> {

                    LiveRoomsScreen(
                        onRequireLogin = {
                            showAuth = true
                        },
                        onCoinsChanged = {
                            coins = it
                        }
                    )
                }

                WaveTab.CREATE -> {

                    CreateScreen(
                        loggedIn = loggedIn,
                        onRequireLogin = {
                            showAuth = true
                        }
                    )
                }

                WaveTab.INBOX -> {
                    InboxScreen()
                }

                WaveTab.PROFILE -> {

                    ProfileScreen(
                        user = currentUser,
                        coins = coins,
                        onLogout = {

                            scope.launch {

                                WaveApi.logout(context)

                                loggedIn = false
                                currentUser = null
                                coins = 0
                                showAuth = true
                            }
                        },
                        onCoinsChanged = {
                            coins = it
                        }
                    )
                }
            }
        }

        WaveBottomBar(
            selected = selectedTab,
            onSelected = {
                selectedTab = it
            }
        )
    }
}

/* ========================================================= */
/* HOME */
/* ========================================================= */

@Composable
private fun HomeScreen(
    coins: Int,
    onOpenLogin: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videos by remember {
        mutableStateOf<List<WaveApi.Video>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        loading = true

        val result = WaveApi.feed(
            context = context,
            limit = 30,
            cursor = 0
        )

        if (result.isSuccess) {

            videos = result.getOrNull().orEmpty()
            error = ""

        } else {

            error =
                result.exceptionOrNull()?.message
                    ?: "تعذر تحميل الفيديوهات"
        }

        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "WAVE",
                    color = WaveWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Live • Video • Music",
                    color = WaveMuted,
                    fontSize = 12.sp
                )
            }

            CoinBadge(coins)
        }

        when {

            loading -> {

                CenterMessage(
                    "جارِ التحميل",
                    "نتصل بخادم Wave..."
                )
            }

            error.isNotBlank() -> {

                CenterMessage(
                    "تعذر تحميل المحتوى",
                    error
                )
            }

            videos.isEmpty() -> {

                CenterMessage(
                    "لا توجد فيديوهات",
                    "ابدأ بنشر أول فيديو على Wave."
                )
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {

                    items(
                        items = videos,
                        key = { it.id }
                    ) { video ->

                        VideoCard(
                            video = video,
                            onLike = {

                                if (!WaveApi.isLoggedIn(context)) {

                                    onOpenLogin()

                                } else {

                                    scope.launch {
                                        WaveApi.likeVideo(
                                            context,
                                            video.id
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: WaveApi.Video,
    onLike: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        )
    ) {

        Column {

            if (video.videoUrl.isNotBlank()) {

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp
                            )
                        ),
                    factory = { ctx ->

                        VideoView(ctx).apply {

                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                            setVideoURI(
                                Uri.parse(video.videoUrl)
                            )

                            setOnPreparedListener {
                                it.isLooping = true
                                it.start()
                            }
                        }
                    }
                )

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "VIDEO",
                        color = WaveMuted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text = video.displayName.ifBlank {
                        "@${video.username}"
                    },
                    color = WaveWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (video.caption.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = video.caption,
                        color = WaveWhite,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!video.musicName.isNullOrBlank()) {

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "♪ ${video.musicName}",
                        color = WavePurple,
                        fontSize = 13.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    TextButton(
                        onClick = onLike
                    ) {

                        Text(
                            text =
                                if (video.liked) {
                                    "♥ ${video.likes}"
                                } else {
                                    "♡ ${video.likes}"
                                },
                            color =
                                if (video.liked) {
                                    WavePink
                                } else {
                                    WaveWhite
                                }
                        )
                    }

                    Text(
                        text = "💬 ${video.comments}",
                        color = WaveMuted
                    )

                    Text(
                        text = "↗ ${video.shares}",
                        color = WaveMuted
                    )

                    Text(
                        text = "${video.views} مشاهدة",
                        color = WaveMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/* ========================================================= */
/* LIVE */
/* ========================================================= */

@Composable
private fun LiveRoomsScreen(
    onRequireLogin: () -> Unit,
    onCoinsChanged: (Int) -> Unit
) {

    val context = LocalContext.current

    var lives by remember {
        mutableStateOf<List<WaveApi.Live>>(emptyList())
    }

    var gifts by remember {
        mutableStateOf<List<WaveApi.Gift>>(emptyList())
    }

    var selectedLive by remember {
        mutableStateOf<WaveApi.Live?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        val livesResult =
            WaveApi.getLives(context)

        if (livesResult.isSuccess) {
            lives =
                livesResult.getOrNull().orEmpty()
        }

        val giftsResult =
            WaveApi.getGifts(context)

        if (giftsResult.isSuccess) {
            gifts =
                giftsResult.getOrNull().orEmpty()
        }

        loading = false
    }

    if (selectedLive != null) {

        LiveRoomScreen(
            live = selectedLive!!,
            gifts = gifts,
            onBack = {
                selectedLive = null
            },
            onCoinsChanged = onCoinsChanged,
            onRequireLogin = onRequireLogin
        )

        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Text(
            text = "البث المباشر",
            color = WaveWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(16.dp)
        )

        when {

            loading -> {

                CenterMessage(
                    "جارِ التحميل",
                    "نبحث عن البثوث الحالية..."
                )
            }

            lives.isEmpty() -> {

                CenterMessage(
                    "لا يوجد بث مباشر الآن",
                    "يمكنك بدء بث جديد من تبويب إنشاء."
                )
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {

                    items(
                        lives,
                        key = { it.id }
                    ) { live ->

                        LiveRoomCard(
                            live = live,
                            onClick = {
                                selectedLive = live
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRoomCard(
    live: WaveApi.Live,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 7.dp
            )
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AvatarCircle(live.displayName)

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = live.displayName,
                        color = WaveWhite,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "@${live.username}",
                        color = WaveMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "LIVE",
                    color = WaveWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            WaveRed,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        )
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = live.title,
                color = WaveWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "مشاهدون: ${live.viewerCount} • إعجابات: ${live.likes}",
                color = WaveMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun LiveRoomScreen(
    live: WaveApi.Live,
    gifts: List<WaveApi.Gift>,
    onBack: () -> Unit,
    onCoinsChanged: (Int) -> Unit,
    onRequireLogin: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentLive by remember {
        mutableStateOf(live)
    }

    var showGifts by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WaveBackground)
                .padding(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {

                Text(
                    text = "رجوع",
                    color = WaveWhite
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = currentLive.displayName,
                    color = WaveWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentLive.title,
                    color = WaveMuted,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "${currentLive.viewerCount}",
                color = WaveWhite
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {

            val playback =
                currentLive.playbackUrl
                    ?: currentLive.streamUrl

            if (!playback.isNullOrBlank()) {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->

                        VideoView(ctx).apply {

                            setVideoURI(
                                Uri.parse(playback)
                            )

                            setOnPreparedListener {
                                it.isLooping = true
                                it.start()
                            }
                        }
                    }
                )

            } else {

                Text(
                    text = "LIVE",
                    color = WaveRed,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WaveBackground)
                .padding(12.dp)
        ) {

            Text(
                text =
                    "مشاهدون ${currentLive.viewerCount} • إعجابات ${currentLive.likes}",
                color = WaveMuted,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {

                        if (!WaveApi.isLoggedIn(context)) {

                            onRequireLogin()

                        } else {

                            scope.launch {

                                val result =
                                    WaveApi.getLive(
                                        context,
                                        currentLive.id
                                    )

                                if (result.isSuccess) {

                                    currentLive =
                                        result.getOrNull()
                                            ?: currentLive
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePurple
                    )
                ) {

                    Text("تحديث")
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {

                        if (!WaveApi.isLoggedIn(context)) {
                            onRequireLogin()
                        } else {
                            showGifts = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePink
                    )
                ) {

                    Text("الهدايا")
                }
            }

            if (message.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = message,
                    color = WaveGreen
                )
            }
        }
    }

    if (showGifts) {

        GiftSheet(
            gifts = gifts,
            onClose = {
                showGifts = false
            },
            onSend = { gift ->

                scope.launch {

                    val result =
                        WaveApi.sendGift(
                            context = context,
                            liveId = currentLive.id,
                            giftId = gift.id
                        )

                    if (result.isSuccess) {

                        onCoinsChanged(
                            result.getOrNull()
                                ?.remainingCoins
                                ?: 0
                        )

                        message =
                            "تم إرسال ${gift.name}"

                    } else {

                        message =
                            result.exceptionOrNull()
                                ?.message
                                ?: "تعذر إرسال الهدية"
                    }

                    showGifts = false
                }
            }
        )
    }
}

/* ========================================================= */
/* GIFTS */
/* ========================================================= */

@Composable
private fun GiftSheet(
    gifts: List<WaveApi.Gift>,
    onClose: () -> Unit,
    onSend: (WaveApi.Gift) -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = WaveBackground
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "هدايا Wave",
                    color = WaveWhite,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onClose
                ) {

                    Text(
                        text = "إغلاق",
                        color = WaveWhite
                    )
                }
            }

            if (gifts.isEmpty()) {

                CenterMessage(
                    "لا توجد هدايا",
                    "سيتم تحميل الهدايا من الخادم."
                )

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {

                    items(
                        gifts,
                        key = { it.id }
                    ) { gift ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    onSend(gift)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = WaveSurface
                            ),
                            shape =
                                RoundedCornerShape(16.dp)
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(15.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text = gift.icon,
                                    fontSize = 30.sp,
                                    modifier =
                                        Modifier.size(48.dp)
                                )

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = gift.name,
                                        color = WaveWhite,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        text =
                                            "${gift.price} Coin",
                                        color = WaveGold,
                                        fontSize = 13.sp
                                    )
                                }

                                Text(
                                    text = "إرسال",
                                    color = WavePink,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ========================================================= */
/* CREATE */
/* ========================================================= */

@Composable
private fun CreateScreen(
    loggedIn: Boolean,
    onRequireLogin: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember {
        mutableStateOf("")
    }

    var createdLive by remember {
        mutableStateOf<WaveApi.Live?>(null)
    }

    var music by remember {
        mutableStateOf<List<WaveApi.MusicTrack>>(emptyList())
    }

    var effects by remember {
        mutableStateOf<List<WaveApi.VisualEffect>>(emptyList())
    }

    var message by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        val musicResult =
            WaveApi.getMusic(context)

        if (musicResult.isSuccess) {
            music =
                musicResult.getOrNull().orEmpty()
        }

        val effectsResult =
            WaveApi.getEffects(context)

        if (effectsResult.isSuccess) {
            effects =
                effectsResult.getOrNull().orEmpty()
        }
    }

    if (createdLive != null) {

        CreatedLiveScreen(
            live = createdLive!!,
            onFinish = {

                scope.launch {

                    WaveApi.updateLive(
                        context = context,
                        liveId = createdLive!!.id,
                        status = "ended"
                    )

                    createdLive = null
                }
            }
        )

        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {

            Text(
                text = "إنشاء",
                color = WaveWhite,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    "ابدأ بث مباشر واستخدم الموسيقى والمؤثرات المتاحة.",
                color = WaveMuted,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = {
                    title = it
                },
                label = {
                    Text("عنوان البث")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {

                    if (!loggedIn) {

                        onRequireLogin()
                        return@Button
                    }

                    if (title.isBlank()) {

                        message =
                            "اكتب عنوان البث أولاً"
                        return@Button
                    }

                    scope.launch {

                        message =
                            "جارِ إنشاء البث..."

                        val result =
                            WaveApi.createLive(
                                context = context,
                                title = title.trim()
                            )

                        if (result.isSuccess) {

                            createdLive =
                                result.getOrNull()

                            message = ""

                        } else {

                            message =
                                result.exceptionOrNull()
                                    ?.message
                                    ?: "تعذر إنشاء البث"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {

                Text("بدء بث مباشر")
            }

            if (message.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = message,
                    color = WaveRed
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            SectionTitle("مكتبة الموسيقى")

            if (music.isEmpty()) {

                Text(
                    text =
                        "لا توجد موسيقى منشورة حاليًا.",
                    color = WaveMuted
                )

            } else {

                music.take(20).forEach {
                    MusicRow(it)
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            SectionTitle("المؤثرات البصرية")

            if (effects.isEmpty()) {

                Text(
                    text =
                        "لا توجد مؤثرات منشورة حاليًا.",
                    color = WaveMuted
                )

            } else {

                effects.forEach {
                    EffectRow(it)
                }
            }
        }
    }
}

@Composable
private fun CreatedLiveScreen(
    live: WaveApi.Live,
    onFinish: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        Text(
            text = "WAVE LIVE",
            color = WaveWhite,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = live.title,
            color = WaveWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Text(
            text = "حالة البث: ${live.status}",
            color = WaveGreen
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        if (!live.rtmpsUrl.isNullOrBlank()) {

            InfoBox(
                "RTMPS URL",
                live.rtmpsUrl!!
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (!live.streamKey.isNullOrBlank()) {

            InfoBox(
                "Stream Key",
                live.streamKey!!
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(
                containerColor = WaveRed
            )
        ) {

            Text("إنهاء البث")
        }
    }
}

/* ========================================================= */
/* MUSIC / EFFECTS */
/* ========================================================= */

@Composable
private fun MusicRow(
    track: WaveApi.MusicTrack
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        )
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "♪",
                color = WavePurple,
                fontSize = 28.sp
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = track.title,
                    color = WaveWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = track.artist,
                    color = WaveMuted,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "${track.durationSeconds}s",
                color = WaveMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EffectRow(
    effect: WaveApi.VisualEffect
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        )
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(WavePurple),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "FX",
                    color = WaveWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = effect.name,
                    color = WaveWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "${effect.type} • ${effect.value}",
                    color = WaveMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/* ========================================================= */
/* INBOX */
/* ========================================================= */

@Composable
private fun InboxScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Text(
            text = "Inbox",
            color = WaveWhite,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WaveSurface
            ),
            shape = RoundedCornerShape(18.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "الإشعارات",
                    color = WaveWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        "الإشعارات والهدايا والمتابعون ستظهر هنا.",
                    color = WaveMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/* ========================================================= */
/* PROFILE */
/* ========================================================= */

@Composable
private fun ProfileScreen(
    user: WaveApi.User?,
    coins: Int,
    onLogout: () -> Unit,
    onCoinsChanged: (Int) -> Unit
) {

    if (user == null) {

        CenterMessage(
            "الحساب",
            "سجل الدخول للوصول إلى حسابك."
        )

        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wallet by remember {
        mutableStateOf<WaveApi.Wallet?>(null)
    }

    var deposits by remember {
        mutableStateOf<List<WaveApi.Deposit>>(emptyList())
    }

    var showWallet by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(showWallet) {

        if (showWallet) {

            val walletResult =
                WaveApi.getWallet(context)

            if (walletResult.isSuccess) {

                wallet =
                    walletResult.getOrNull()

                onCoinsChanged(
                    wallet?.coins ?: coins
                )
            }

            val depositsResult =
                WaveApi.getWalletDeposits(context)

            if (depositsResult.isSuccess) {

                deposits =
                    depositsResult.getOrNull()
                        .orEmpty()
            }
        }
    }

    if (showWallet) {

        WalletScreen(
            wallet = wallet,
            deposits = deposits,
            onBack = {
                showWallet = false
            },
            onDeposit = { amount, number, reference ->

                scope.launch {

                    val result =
                        WaveApi.createWalletDeposit(
                            context = context,
                            amount = amount,
                            walletNumber = number,
                            transactionReference =
                                reference
                        )

                    if (result.isSuccess) {

                        val refresh =
                            WaveApi.getWallet(context)

                        if (refresh.isSuccess) {

                            val data =
                                refresh.getOrNull()

                            wallet = data

                            onCoinsChanged(
                                data?.coins ?: coins
                            )
                        }
                    }
                }
            }
        )

        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {

            AvatarCircle(
                user.displayName,
                82.dp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = user.displayName,
                color = WaveWhite,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "@${user.username}",
                color = WaveMuted
            )

            if (!user.bio.isNullOrBlank()) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = user.bio!!,
                    color = WaveWhite
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                StatBox(
                    user.followers,
                    "المتابعون"
                )

                StatBox(
                    user.following,
                    "يتابع"
                )

                StatBox(
                    coins,
                    "Coins"
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showWallet = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {

                Text("محفظة Wave")
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLogout
            ) {

                Text(
                    text = "تسجيل الخروج",
                    color = WaveWhite
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text =
                    if (user.verified) {
                        "✓ حساب موثق"
                    } else {
                        "حساب عادي"
                    },
                color =
                    if (user.verified) {
                        WaveGreen
                    } else {
                        WaveMuted
                    }
            )
        }
    }
}

/* ========================================================= */
/* WALLET */
/* ========================================================= */

@Composable
private fun WalletScreen(
    wallet: WaveApi.Wallet?,
    deposits: List<WaveApi.Deposit>,
    onBack: () -> Unit,
    onDeposit: (
        Int,
        String,
        String
    ) -> Unit
) {

    var amount by remember {
        mutableStateOf("")
    }

    var walletNumber by remember {
        mutableStateOf(
            wallet?.walletNumbers
                ?.firstOrNull()
                ?: ""
        )
    }

    var reference by remember {
        mutableStateOf("")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {

            TextButton(
                onClick = onBack
            ) {

                Text(
                    "رجوع",
                    color = WaveWhite
                )
            }

            Text(
                text = "محفظة Wave",
                color = WaveWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveSurface
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        "الرصيد",
                        color = WaveMuted
                    )

                    Text(
                        "${wallet?.coins ?: 0} Coins",
                        color = WaveGold,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            SectionTitle("إضافة رصيد")

            Text(
                text =
                    "الدفع المتاح حاليًا عبر المحفظة فقط.",
                color = WaveMuted,
                fontSize = 12.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amount,
                onValueChange = {
                    amount =
                        it.filter { c ->
                            c.isDigit()
                        }
                },
                label = {
                    Text("المبلغ")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = walletNumber,
                onValueChange = {
                    walletNumber = it
                },
                label = {
                    Text("رقم المحفظة")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = reference,
                onValueChange = {
                    reference = it
                },
                label = {
                    Text("رقم العملية / المرجع")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {

                    val value =
                        amount.toIntOrNull()
                            ?: return@Button

                    if (
                        value > 0 &&
                        walletNumber.isNotBlank()
                    ) {

                        onDeposit(
                            value,
                            walletNumber,
                            reference
                        )

                        amount = ""
                        reference = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {

                Text("إرسال طلب الإيداع")
            }

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            SectionTitle("طلبات الإيداع")

            if (deposits.isEmpty()) {

                Text(
                    "لا توجد عمليات سابقة.",
                    color = WaveMuted
                )

            } else {

                deposits.take(20).forEach {
                    DepositRow(it)
                }
            }
        }
    }
}

/* ========================================================= */
/* AUTH */
/* ========================================================= */

@Composable
private fun AuthScreen(
    onLoginSuccess: (WaveApi.User?) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var registerMode by remember {
        mutableStateOf(false)
    }

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var displayName by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveBackground)
            .padding(22.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(45.dp)
        )

        Text(
            text = "WAVE",
            color = WaveWhite,
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "LIVE",
            color = WavePurple,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(35.dp)
        )

        Text(
            text =
                if (registerMode) {
                    "إنشاء حساب حقيقي"
                } else {
                    "تسجيل الدخول"
                },
            color = WaveWhite,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (registerMode) {

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = displayName,
                onValueChange = {
                    displayName = it
                },
                label = {
                    Text("الاسم")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = username,
            onValueChange = {
                username = it
            },
            label = {
                Text("Username")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Password")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = {

                if (
                    username.isBlank() ||
                    password.isBlank()
                ) {

                    error =
                        "أدخل اسم المستخدم وكلمة المرور"
                    return@Button
                }

                if (
                    registerMode &&
                    displayName.isBlank()
                ) {

                    error =
                        "أدخل الاسم"
                    return@Button
                }

                scope.launch {

                    loading = true
                    error = ""

                    val result =
                        if (registerMode) {

                            WaveApi.register(
                                context = context,
                                username = username.trim(),
                                password = password,
                                displayName =
                                    displayName.trim()
                            )

                        } else {

                            WaveApi.login(
                                context = context,
                                username = username.trim(),
                                password = password
                            )
                        }

                    if (result.isSuccess) {

                        onLoginSuccess(
                            result.getOrNull()?.user
                        )

                    } else {

                        error =
                            result.exceptionOrNull()
                                ?.message
                                ?: "حدث خطأ"
                    }

                    loading = false
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = WavePurple
            )
        ) {

            Text(
                text =
                    if (loading) {
                        "جارِ الاتصال..."
                    } else if (registerMode) {
                        "إنشاء الحساب"
                    } else {
                        "دخول"
                    }
            )
        }

        if (error.isNotBlank()) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = error,
                color = WaveRed,
                fontSize = 13.sp
            )
        }

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        TextButton(
            onClick = {

                registerMode = !registerMode
                error = ""
            }
        ) {

            Text(
                text =
                    if (registerMode) {
                        "لدي حساب بالفعل"
                    } else {
                        "إنشاء حساب جديد"
                    },
                color = WavePurple
            )
        }
    }
}

/* ========================================================= */
/* BOTTOM NAVIGATION */
/* ========================================================= */

@Composable
private fun WaveBottomBar(
    selected: WaveTab,
    onSelected: (WaveTab) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WaveSurface)
            .padding(
                horizontal = 4.dp,
                vertical = 7.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {

        BottomItem(
            "الرئيسية",
            "H",
            selected == WaveTab.HOME
        ) {
            onSelected(WaveTab.HOME)
        }

        BottomItem(
            "LIVE",
            "L",
            selected == WaveTab.LIVE
        ) {
            onSelected(WaveTab.LIVE)
        }

        BottomItem(
            "إنشاء",
            "+",
            selected == WaveTab.CREATE
        ) {
            onSelected(WaveTab.CREATE)
        }

        BottomItem(
            "Inbox",
            "M",
            selected == WaveTab.INBOX
        ) {
            onSelected(WaveTab.INBOX)
        }

        BottomItem(
            "حسابي",
            "P",
            selected == WaveTab.PROFILE
        ) {
            onSelected(WaveTab.PROFILE)
        }
    }
}

@Composable
private fun BottomItem(
    text: String,
    short: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 7.dp,
                vertical = 4.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = short,
            color =
                if (selected) {
                    WavePurple
                } else {
                    WaveMuted
                },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = text,
            color =
                if (selected) {
                    WaveWhite
                } else {
                    WaveMuted
                },
            fontSize = 10.sp
        )
    }
}

/* ========================================================= */
/* UI HELPERS */
/* ========================================================= */

@Composable
private fun CoinBadge(
    coins: Int
) {

    Row(
        modifier = Modifier
            .background(
                WaveSurface,
                RoundedCornerShape(12.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "●",
            color = WaveGold,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.width(5.dp)
        )

        Text(
            text = coins.toString(),
            color = WaveWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AvatarCircle(
    name: String,
    size: androidx.compose.ui.unit.Dp = 52.dp
) {

    val letter =
        name.trim()
            .firstOrNull()
            ?.uppercase()
            ?.toString()
            ?: "W"

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(WavePurple),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = letter,
            color = WaveWhite,
            fontSize =
                if (size > 60.dp) {
                    25.sp
                } else {
                    17.sp
                },
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun CenterMessage(
    title: String,
    message: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = title,
            color = WaveWhite,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message,
            color = WaveMuted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SectionTitle(
    title: String
) {

    Text(
        text = title,
        color = WaveWhite,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            vertical = 7.dp
        )
    )
}

@Composable
private fun StatBox(
    value: Int,
    label: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = value.toString(),
            color = WaveWhite,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = WaveMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun InfoBox(
    title: String,
    value: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                text = title,
                color = WavePurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = value,
                color = WaveWhite,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DepositRow(
    deposit: WaveApi.Deposit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        )
    ) {

        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "${deposit.amount} — ${deposit.coins} Coins",
                    color = WaveWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = deposit.walletNumber,
                    color = WaveMuted,
                    fontSize = 11.sp
                )
            }

            Text(
                text = deposit.status,
                color =
                    when (
                        deposit.status.lowercase()
                    ) {
                        "approved",
                        "completed",
                        "success" -> WaveGreen

                        "rejected",
                        "failed" -> WaveRed

                        else -> WaveGold
                    },
                fontSize = 12.sp
            )
        }
    }
}
