package com.vyro.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF08060D)
private val Card = Color(0xFF15111D)
private val Purple = Color(0xFF9B5CFF)
private val Pink = Color(0xFFFF4FA3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaveLiveApp()
        }
    }
}

@Composable
fun WaveLiveApp() {
    var started by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Card,
            primary = Purple
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Bg
        ) {
            if (!started) {
                Splash {
                    started = true
                }
            } else {
                MainScreen()
            }
        }
    }
}

@Composable
fun Splash(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05030A),
                        Color(0xFF160B22)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WAVE",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Wave Live",
                color = Color.LightGray,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(34.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Text(
                    "ابدأ الآن",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

enum class Tab(
    val title: String,
    val icon: String
) {
    FOR_YOU("لك", "⌂"),
    LIVE("LIVE", "●"),
    CREATE("إنشاء", "+"),
    INBOX("الرسائل", "✉"),
    PROFILE("حسابي", "●")
}

data class VideoItem(
    val title: String,
    val creator: String,
    val uri: String? = null
)

@Composable
fun MainScreen() {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "wave_user",
            Context.MODE_PRIVATE
        )
    }

    var tab by remember {
        mutableStateOf(Tab.FOR_YOU)
    }

    var coins by remember {
        mutableIntStateOf(
            prefs.getInt("coins", 1200)
        )
    }

    var earnings by remember {
        mutableIntStateOf(
            prefs.getInt("earnings", 0)
        )
    }

    var loggedIn by remember {
        mutableStateOf(
            prefs.getBoolean("logged_in", false)
        )
    }

    var userName by remember {
        mutableStateOf(
            prefs.getString("name", "") ?: ""
        )
    }

    var userHandle by remember {
        mutableStateOf(
            prefs.getString("handle", "") ?: ""
        )
    }

    var showWallet by remember {
        mutableStateOf(false)
    }

    val videos = remember {
        mutableStateListOf(
            VideoItem(
                "أول فيديو على Wave Live",
                "Wave Creator"
            ),
            VideoItem(
                "استكشف الفيديوهات والبث المباشر",
                "Wave Live"
            ),
            VideoItem(
                "مرحبا بكم في Wave",
                "Wave Team"
            )
        )
    }

    fun saveCoins(value: Int) {
        coins = value
        prefs.edit()
            .putInt("coins", value)
            .apply()
    }

    fun saveEarnings(value: Int) {
        earnings = value
        prefs.edit()
            .putInt("earnings", value)
            .apply()
    }

    Scaffold(
        containerColor = Bg,

        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0E0A14)
            ) {
                Tab.values().forEach { item ->

                    NavigationBarItem(
                        selected = tab == item,
                        onClick = {
                            tab = item
                        },

                        icon = {
                            Text(
                                item.icon,
                                fontSize = 20.sp
                            )
                        },

                        label = {
                            Text(
                                item.title,
                                fontSize = 10.sp
                            )
                        },

                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor =
                                    Purple.copy(alpha = 0.35f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                    )
                }
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when (tab) {

                Tab.FOR_YOU -> {
                    ForYouScreen(videos)
                }

                Tab.LIVE -> {
                    LiveScreen(
                        coins = coins,

                        onWallet = {
                            showWallet = true
                        },

                        onGift = { price ->

                            if (coins >= price) {

                                saveCoins(
                                    coins - price
                                )

                                saveEarnings(
                                    earnings + price
                                )
                            }
                        }
                    )
                }

                Tab.CREATE -> {
                    CreatorCenter(
                        loggedIn = loggedIn,

                        onLogin = {
                            tab = Tab.PROFILE
                        },

                        onVideoPublished = { video ->
                            videos.add(0, video)
                        },

                        earnings = earnings,

                        onWallet = {
                            showWallet = true
                        }
                    )
                }

                Tab.INBOX -> {
                    InboxScreen()
                }

                Tab.PROFILE -> {
                    ProfileScreen(
                        loggedIn = loggedIn,
                        name = userName,
                        handle = userHandle,
                        earnings = earnings,

                        onRegister = { name, handle ->

                            userName = name
                            userHandle = handle
                            loggedIn = true

                            prefs.edit()
                                .putBoolean(
                                    "logged_in",
                                    true
                                )
                                .putString(
                                    "name",
                                    name
                                )
                                .putString(
                                    "handle",
                                    handle
                                )
                                .apply()
                        },

                        onLogout = {

                            loggedIn = false
                            userName = ""
                            userHandle = ""

                            prefs.edit()
                                .clear()
                                .apply()

                            coins = 1200
                            earnings = 0
                        },

                        onCreatorWallet = {
                            tab = Tab.CREATE
                        }
                    )
                }
            }

            if (showWallet) {

                WalletDialog(
                    coins = coins,

                    onClose = {
                        showWallet = false
                    },

                    onAdd = { amount ->

                        saveCoins(
                            coins + amount
                        )

                        showWallet = false
                    }
                )
            }
        }
    }
}

@Composable
fun ForYouScreen(
    videos: List<VideoItem>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),

        contentPadding = PaddingValues(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        "Wave Live",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        "لك • فيديوهات وبث مباشر",
                        color = Color.Gray
                    )
                }

                Text(
                    "FOR YOU",
                    color = Pink,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(videos) { video ->

            VideoCard(video)
        }
    }
}

@Composable
fun VideoCard(
    video: VideoItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF32164A),
                            Color(0xFF0B0810)
                        )
                    )
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                "WAVE VIDEO",
                color = Color.Gray,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Purple, Pink)
                        )
                    )
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    video.creator,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    video.title,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    "♡",
                    fontSize = 26.sp
                )

                Text(
                    "إعجاب",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun LiveScreen(
    coins: Int,
    onWallet: () -> Unit,
    onGift: (Int) -> Unit
) {
    var selectedGift by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100B18))
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    "LIVE",
                    color = Pink,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "Wave Live",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onWallet
            ) {
                Text("🪙 $coins")
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF29163D),
                            Color(0xFF0B0810)
                        )
                    )
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    "🔴",
                    fontSize = 38.sp
                )

                Text(
                    "LIVE VIDEO",
                    color = Color.Gray,
                    fontSize = 22.sp
                )

                Text(
                    "البث المباشر",
                    color = Color.DarkGray
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Text(
            "🎁 أرسل هدية",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        val gifts = listOf(
            "👑" to ("تاج Wave" to 100),
            "💎" to ("ألماسة" to 250),
            "🚀" to ("صاروخ" to 500),
            "🌟" to ("نجمة" to 1000)
        )

        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            items(gifts) { item ->

                val emoji = item.first
                val name = item.second.first
                val price = item.second.second

                Column(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Card)
                        .clickable {

                            selectedGift = name

                            onGift(price)
                        }
                        .padding(10.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        emoji,
                        fontSize = 34.sp
                    )

                    Text(
                        name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "$price 🪙",
                        fontSize = 11.sp,
                        color = Purple
                    )
                }
            }
        }

        if (selectedGift != null) {

            Text(
                "تم إرسال $selectedGift",
                color = Purple,
                modifier = Modifier.padding(
                    top = 8.dp
                )
            )
        }
    }
}

@Composable
fun CreatorCenter(
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onVideoPublished: (VideoItem) -> Unit,
    earnings: Int,
    onWallet: () -> Unit
) {
    if (!loggedIn) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,

                modifier = Modifier.padding(24.dp)
            ) {

                Text(
                    "لوحة المبدع",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    "أنشئ حسابًا أولًا لرفع الفيديوهات وإدارة الأرباح.",
                    color = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = onLogin
                ) {
                    Text("إضافة حساب")
                }
            }
        }

        return
    }

    var showUpload by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            "لوحة المبدع",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            "إدارة المحتوى والأرباح",
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            CreatorStat(
                "💰",
                "الأرباح",
                earnings.toString()
            )

            CreatorStat(
                "🎬",
                "المحتوى",
                "Wave"
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
                showUpload = true
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Purple
            )
        ) {
            Text(
                "🎥 رفع فيديو",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onWallet,

            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("💰 محفظة أرباح المبدع")
        }

        if (showUpload) {

            UploadVideoDialog(
                onClose = {
                    showUpload = false
                },

                onPublish = { video ->

                    onVideoPublished(video)

                    showUpload = false
                }
            )
        }
    }
}

@Composable
fun CreatorStat(
    icon: String,
    title: String,
    value: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .padding(16.dp)
    ) {

        Text(
            icon,
            fontSize = 25.sp
        )

        Text(
            title,
            color = Color.Gray,
            fontSize = 12.sp
        )

        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UploadVideoDialog(
    onClose: () -> Unit,
    onPublish: (VideoItem) -> Unit
) {
    val context = LocalContext.current

    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var title by remember {
        mutableStateOf("")
    }

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            selectedUri = uri
        }

    AlertDialog(
        onDismissRequest = onClose,

        title = {
            Text("رفع فيديو")
        },

        text = {

            Column {

                Button(
                    onClick = {
                        picker.launch("video/*")
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        if (selectedUri == null)
                            "اختيار فيديو من الهاتف"
                        else
                            "تم اختيار الفيديو ✓"
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = title,

                    onValueChange = {
                        title = it
                    },

                    modifier = Modifier.fillMaxWidth(),

                    label = {
                        Text("عنوان الفيديو")
                    },

                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    "سي
