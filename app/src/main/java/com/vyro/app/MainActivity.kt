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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class LiveCreator(
    val name: String,
    val viewers: String,
    val title: String
)

private data class VideoItem(
    val creator: String,
    val title: String,
    val likes: Int
)

private data class GiftItem(
    val icon: String,
    val name: String,
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
            onLogin = {
                loggedIn = true
            }
        )
    }
}

@Composable
private fun LoginScreen(
    onLogin: () -> Unit
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
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "WAVE LIVE",
                color = WavePurple,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "بث مباشر وفيديوهات ومجتمعك في مكان واحد",
                color = Color.LightGray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(35.dp)
            )

            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "تسجيل الدخول",
                    fontSize = 17.sp
                )
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

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "ابدأ رحلتك مع Wave Live",
                color = Color.Gray,
                fontSize = 13.sp
            )
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
                Column {

                    Text(
                        text = "أنشئ حسابك لبدء متابعة الفيديوهات والبث المباشر."
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "حساب جديد",
                        color = WavePurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegister = false
                        onLogin()
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

    var selectedTab by remember {
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
                    selected = selectedTab == WaveTab.HOME,
                    onClick = {
                        selectedTab = WaveTab.HOME
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "الرئيسية"
                        )
                    },
                    label = {
                        Text("الرئيسية")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.LIVE,
                    onClick = {
                        selectedTab = WaveTab.LIVE
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "مباشر"
                        )
                    },
                    label = {
                        Text("مباشر")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.CREATE,
                    onClick = {
                        selectedTab = WaveTab.CREATE
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إنشاء"
                        )
                    },
                    label = {
                        Text("إنشاء")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.INBOX,
                    onClick = {
                        selectedTab = WaveTab.INBOX
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "الرسائل"
                        )
                    },
                    label = {
                        Text("الرسائل")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.PROFILE,
                    onClick = {
                        selectedTab = WaveTab.PROFILE
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
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

        when (selectedTab) {

            WaveTab.HOME -> {
                HomeScreen(
                    modifier = Modifier.padding(paddingValues),
                    coins = coins,
                    onWallet = {
                        showWallet = true
                    },
                    onLive = {
                        selectedTab = WaveTab.LIVE
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
                        imageVector = Icons.Default.LiveTv,
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
                        imageVector = Icons.Default.VideoLibrary,
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
    modifier: Modifier
