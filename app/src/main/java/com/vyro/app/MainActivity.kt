package com.vyro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WaveBackground = Color(0xFF07060B)
private val WaveCard = Color(0xFF15121C)
private val WavePurple = Color(0xFF9B5CFF)
private val WavePink = Color(0xFFFF4FA3)
private val WaveGreen = Color(0xFF20D889)
private val WaveGold = Color(0xFFFFC857)

private val WaveColors = darkColorScheme(
    primary = WavePurple,
    secondary = WavePink,
    background = WaveBackground,
    surface = WaveCard,
    onBackground = Color.White,
    onSurface = Color.White
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = WaveColors
            ) {
                WaveLiveApp()
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

private data class VideoItem(
    val creator: String,
    val title: String,
    val likes: Int
)

private data class LiveCreator(
    val name: String,
    val viewers: String,
    val title: String
)

private data class GiftItem(
    val name: String,
    val icon: String,
    val price: Int
)

@Composable
private fun WaveLiveApp() {

    var loggedIn by remember {
        mutableStateOf(false)
    }

    if (loggedIn) {
        MainWaveScreen()
    } else {
        LoginScreen(
            onContinue = {
                loggedIn = true
            }
        )
    }
}

@Composable
private fun LoginScreen(
    onContinue: () -> Unit
) {

    var showRegister by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveBackground),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "WAVE",
                color = Color.White,
                fontSize = 50.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "WAVE LIVE",
                color = WavePurple,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "بث مباشر وفيديوهات ومجتمعك في مكان واحد",
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("تسجيل الدخول")
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedButton(
                onClick = {
                    showRegister = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("إنشاء حساب جديد")
            }
        }
    }

    if (showRegister) {

        AlertDialog(
            onDismissRequest = {
                showRegister = false
            },

            title = {
                Text("إنشاء حساب Wave")
            },

            text = {
                Text(
                    "ابدأ حسابك على Wave Live لمتابعة البث والفيديوهات وإنشاء المحتوى."
                )
            },

            confirmButton = {

                Button(
                    onClick = {
                        showRegister = false
                        onContinue()
                    }
                ) {
                    Text("متابعة")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showRegister = false
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun MainWaveScreen() {

    var tab by remember {
        mutableStateOf(WaveTab.HOME)
    }

    var coins by remember {
        mutableIntStateOf(1200)
    }

    var showWallet by remember {
        mutableStateOf(false)
    }

    var showLiveRoom by remember {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = WaveBackground,

        bottomBar = {

            NavigationBar(
                containerColor = WaveCard
            ) {

                NavigationBarItem(
                    selected = tab == WaveTab.HOME,
                    onClick = {
                        tab = WaveTab.HOME
                    },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "الرئيسية"
                        )
                    },
                    label = {
                        Text("الرئيسية")
                    }
                )

                NavigationBarItem(
                    selected = tab == WaveTab.LIVE,
                    onClick = {
                        tab = WaveTab.LIVE
                    },
                    icon = {
                        Icon(
                            Icons.Default.LiveTv,
                            contentDescription = "مباشر"
                        )
                    },
                    label = {
                        Text("مباشر")
                    }
                )

                NavigationBarItem(
                    selected = tab == WaveTab.CREATE,
                    onClick = {
                        tab = WaveTab.CREATE
                    },
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "إنشاء"
                        )
                    },
                    label = {
                        Text("إنشاء")
                    }
                )

                NavigationBarItem(
                    selected = tab == WaveTab.INBOX,
                    onClick = {
                        tab = WaveTab.INBOX
                    },
                    icon = {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "الرسائل"
                        )
                    },
                    label = {
                        Text("الرسائل")
                    }
                )

                NavigationBarItem(
                    selected = tab == WaveTab.PROFILE,
                    onClick = {
                        tab = WaveTab.PROFILE
                    },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "حسابي"
                        )
                    },
                    label = {
                        Text("حسابي")
                    }
                )
            }
        }
    ) { paddingValues ->

        when (tab) {

            WaveTab.HOME -> {
                HomeScreen(
                    modifier = Modifier.padding(paddingValues),
                    coins = coins,
                    onWallet = {
                        showWallet = true
                    },
                    onLive = {
                        tab = WaveTab.LIVE
                    }
                )
            }

            WaveTab.LIVE -> {
                LiveScreen(
                    modifier = Modifier.padding(paddingValues),
                    coins = coins,
                    onWallet = {
                        showWallet = true
                    },
                    onOpenLive = {
                        showLiveRoom = true
                    }
                )
            }

            WaveTab.CREATE -> {
                CreateScreen(
                    modifier = Modifier.padding(paddingValues),
                    coins = coins,
                    onWallet = {
                        showWallet = true
                    },
                    onStartLive = {
                        showLiveRoom = true
                    }
                )
            }

            WaveTab.INBOX -> {
                InboxScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }

            WaveTab.PROFILE -> {
                ProfileScreen(
                    modifier = Modifier.padding(paddingValues),
                    coins = coins,
                    onWallet = {
                        showWallet = true
                    }
                )
            }
        }
    }

    if (showWallet) {

        WalletDialog(
            coins = coins,
            onClose = {
                showWallet = false
            },
            onRecharge = { amount ->
                coins += amount
            }
        )
    }

    if (showLiveRoom) {

        LiveRoomDialog(
            coins = coins,
            onClose = {
                showLiveRoom = false
            },
            onGift = { price ->

                if (coins >= price) {
                    coins -= price
                }
            }
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    coins: Int,
    onWallet: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "🪙 $coins",
                color = WaveGold,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onWallet
            ) {

                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "المحفظة",
                    tint = WaveGold
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    coins: Int,
    onWallet: () -> Unit,
    onLive: () -> Unit
) {

    val videos = listOf(
        VideoItem(
            creator = "Wave Creator",
            title = "أهلاً بكم في Wave Live",
            likes = 2450
        ),
        VideoItem(
            creator = "Mona Wave",
            title = "يوم جديد وبث جديد ❤️",
            likes = 1830
        ),
        VideoItem(
            creator = "Ahmed Live",
            title = "شاهد أحدث فيديوهات Wave",
            likes = 970
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
    ) {

        item {

            TopBar(
                title = "WAVE",
                coins = coins,
                onWallet = onWallet
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = onLive,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePink
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text("شاهد مباشر")
                }

                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text("الفيديوهات")
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "الفيديوهات المقترحة",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }

        items(videos) { video ->
            VideoCard(video)
        }
    }
}

@Composable
private fun VideoCard(
    video: VideoItem
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 7.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor = WaveCard
        ),
        shape = RoundedCornerShape(18.dp)
    ) {

        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF211A2D)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = WavePurple,
                    modifier = Modifier.size(64.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(WavePurple),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = video.creator.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = video.creator,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = video.title,
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = WavePink,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = video.likes.toString(),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveScreen(
    modifier: Modifier,
    coins: Int,
    onWallet: () -> Unit,
    onOpenLive: () -> Unit
) {

    val creators = listOf(
        LiveCreator(
            name = "Wave Star",
            viewers = "2.4K",
            title = "ليلة Wave Live"
        ),
        LiveCreator(
            name = "Mona Wave",
            viewers = "1.8K",
            title = "أهلاً بكم ❤️"
        ),
        LiveCreator(
            name = "Ahmed Live",
            viewers = "850",
            title = "جلسة مباشرة"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
    ) {

        item {

            TopBar(
                title = "البث المباشر",
                coins = coins,
                onWallet = onWallet
            )

            Button(
                onClick = onOpenLive,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePink
                ),
                shape = RoundedCornerShape(16.dp)
            ) {

                Icon(
                    Icons.Default.LiveTv,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text("فتح غرفة البث")
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )
        }

        items(creators) { creator ->

            LiveCreatorCard(
                creator = creator,
                onOpenLive = onOpenLive
            )
        }
    }
}

@Composable
private fun LiveCreatorCard(
    creator: LiveCreator,
    onOpenLive: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 7.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor = WaveCard
        ),
        shape = RoundedCornerShape(18.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(WavePink),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = creator.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = creator.title,
                    color = Color.LightGray
                )

                Text(
                    text = "🔴 ${creator.viewers} مشاهدة",
                    color = WavePink,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onOpenLive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {
                Text("دخول")
            }
        }
    }
}

@Composable
private fun CreateScreen(
    modifier: Modifier,
    coins: Int,
    onWallet: () -> Unit,
    onStartLive: () -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
            .padding(16.dp)
    ) {

        TopBar(
            title = "إنشاء",
            coins = coins,
            onWallet = onWallet
        )

        CreateActionCard(
            icon = "🔴",
            title = "ابدأ بث مباشر",
            description = "ابدأ غرفة بث جديدة وتفاعل مع جمهورك.",
            buttonText = "ابدأ البث",
            onClick = onStartLive
        )

        CreateActionCard(
            icon = "🎬",
            title = "رفع فيديو",
            description = "واجهة رفع الفيديو جاهزة للربط بخدمة التخزين.",
            buttonText = "اختيار فيديو",
            onClick = {}
        )

        CreateActionCard(
            icon = "💰",
            title = "أرباح المبدعين",
            description = "تابع أرباح المحتوى والهدايا داخل محفظتك.",
            buttonText = "فتح المحفظة",
            onClick = onWallet
        )
    }
}

@Composable
private fun CreateActionCard(
    icon: String,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveCard
        ),
        shape = RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = icon,
                fontSize = 32.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = description,
                color = Color.LightGray
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun InboxScreen(
    modifier: Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            tint = WavePurple,
            modifier = Modifier.size(64.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "الرسائل والإشعارات",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "ستظهر هنا الرسائل والإشعارات الخاصة بحسابك.",
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    coins: Int,
    onWallet: () -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
            .padding(16.dp)
    ) {

        TopBar(
            title = "حسابي",
            coins = coins,
            onWallet = onWallet
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WaveCard
            ),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(WavePurple),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Wave Creator",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "@wave_creator",
                    color = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    ProfileStat(
                        label = "المتابعون",
                        value = "0"
                    )

                    ProfileStat(
                        label = "الفيديوهات",
                        value = "0"
                    )

                    ProfileStat(
                        label = "الأرباح",
                        value = "0"
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    onClick = onWallet,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WaveGreen
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("محفظة الأرباح والعملات")
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(
    label: String,
    value: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun WalletDialog(
    coins: Int,
    onClose: () -> Unit,
    onRecharge: (Int) -> Unit
) {

    AlertDialog(
        onDismissRequest = onClose,

        title = {
            Text("محفظة Wave")
        },

        text = {

            Column {

                Text(
                    text = "رصيد العملات: 🪙 $coins",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "الشحن متاح من خلال محافظ الدفع فقط."
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text("01284306120")
                Text("01144210918")

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "اختر قيمة تجريبية لإضافة العملات:",
                    color = WavePurple
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            onRecharge(500)
                        }
                    ) {
                        Text("+500")
                    }

                    Button(
                        onClick = {
                            onRecharge(1000)
                        }
                    ) {
                        Text("+1000")
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onClose
            ) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun LiveRoomDialog(
    coins: Int,
    onClose: () -> Unit,
    onGift: (Int) -> Unit
) {

    val gifts = listOf(
        GiftItem(
            name = "تاج Wave",
            icon = "👑",
            price = 100
        ),
        GiftItem(
            name = "قلب ماسي",
            icon = "💎",
            price = 250
        ),
        GiftItem(
            name = "صاروخ",
            icon = "🚀",
            price = 500
        ),
        GiftItem(
            name = "قلعة Wave",
            icon = "🏰",
            price = 1000
        )
    )

    AlertDialog(
        onDismissRequest = onClose,

        title = {
            Text("غرفة Wave Live")
        },

        text = {

            Column {

                Text(
                    text = "غرفة البث المباشر"
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "رصيدك: 🪙 $coins",
                    color = WaveGold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                gifts.forEach { gift ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = gift.icon,
                            fontSize = 28.sp
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = gift.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "🪙 ${gift.price}",
                                color = WaveGold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                onGift(gift.price)
                            },
                            enabled = coins >= gift.price
                        ) {
                            Text("إرسال")
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onClose
            ) {
                Text("خروج")
            }
        }
    )
}
