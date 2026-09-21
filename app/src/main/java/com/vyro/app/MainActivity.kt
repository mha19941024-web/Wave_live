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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val WavePurple = Color(0xFF8B5CF6)
private val WavePink = Color(0xFFEC4899)
private val WaveDark = Color(0xFF09070F)
private val WaveCard = Color(0xFF17121F)
private val WaveText = Color(0xFFF7F3FF)
private val WaveMuted = Color(0xFFAAA1B8)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaveTheme {
                WaveRoot()
            }
        }
    }
}

@Composable
private fun WaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

@Composable
private fun WaveRoot() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1800)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        WaveApp()
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05030A),
                        Color(0xFF170B25),
                        Color(0xFF05030A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(WavePurple, WavePink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "W",
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "WAVE",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Wave Live",
                color = WaveMuted,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "LIVE • MUSIC • FRIENDS",
                color = WavePurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveApp() {
    var selectedTab by remember { mutableStateOf(WaveTab.HOME) }
    var coins by remember { mutableIntStateOf(1250) }

    Scaffold(
        containerColor = WaveDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            WaveTab.HOME -> "WAVE"
                            WaveTab.LIVE -> "LIVE"
                            WaveTab.CREATE -> "Create Live"
                            WaveTab.INBOX -> "Inbox"
                            WaveTab.PROFILE -> "Profile"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        color = WaveText
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(19.dp)
                        )

                        Text(
                            text = coins.toString(),
                            color = WaveText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WaveDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = Color(0xFF100C16)
            ) {
                NavigationBarItem(
                    selected = selectedTab == WaveTab.HOME,
                    onClick = { selectedTab = WaveTab.HOME },
                    icon = {
                        Icon(Icons.Default.Home, "Home")
                    },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.LIVE,
                    onClick = { selectedTab = WaveTab.LIVE },
                    icon = {
                        Icon(Icons.Default.LiveTv, "Live")
                    },
                    label = { Text("LIVE") }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.CREATE,
                    onClick = { selectedTab = WaveTab.CREATE },
                    icon = {
                        Icon(Icons.Default.Add, "Create")
                    },
                    label = { Text("Create") }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.INBOX,
                    onClick = { selectedTab = WaveTab.INBOX },
                    icon = {
                        Icon(Icons.Default.Mail, "Inbox")
                    },
                    label = { Text("Inbox") }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.PROFILE,
                    onClick = { selectedTab = WaveTab.PROFILE },
                    icon = {
                        Icon(Icons.Default.Person, "Profile")
                    },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                WaveTab.HOME -> HomeScreen(
                    onOpenLive = {
                        selectedTab = WaveTab.LIVE
                    }
                )

                WaveTab.LIVE -> LiveScreen(
                    coins = coins,
                    onGiftSent = { price ->
                        if (coins >= price) {
                            coins -= price
                        }
                    }
                )

                WaveTab.CREATE -> CreateLiveScreen()

                WaveTab.INBOX -> InboxScreen()

                WaveTab.PROFILE -> ProfileScreen(
                    coins = coins,
                    onAddCoins = {
                        coins += 500
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenLive: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveDark)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome to Wave 👋",
                color = WaveText,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Watch live streams and meet new people",
                color = WaveMuted,
                fontSize = 14.sp
            )
        }

        item {
            LiveHeroCard(onOpenLive)
        }

        item {
            Text(
                text = "Live Now",
                color = WaveText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            listOf(
                "Mina Live",
                "Sara Music",
                "Ahmed Gaming",
                "Nour Chat"
            )
        ) { name ->
            LiveRoomCard(
                name = name,
                onClick = onOpenLive
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LiveHeroCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveCard
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF4C1D95),
                            Color(0xFF9D174D)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Live",
                    tint = Color.White,
                    modifier = Modifier.size(55.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "GO LIVE",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Join the Wave",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LiveRoomCard(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = WaveCard
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(55.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(WavePurple, WavePink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.first().toString(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = name,
                    color = WaveText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )

                    Text(
                        text = " LIVE now",
                        color = Color(0xFF22C55E),
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Open",
                tint = WavePurple
            )
        }
    }
}

@Composable
private fun LiveScreen(
    coins: Int,
    onGiftSent: (Int) -> Unit
) {
    var showGifts by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF21112D),
                                Color.Black
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(WavePurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "W",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }

                        Column(
                            modifier = Modifier.padding(start = 10.dp)
                        ) {
                            Text(
                                text = "Wave Creator",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "12.4K watching",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = "Live video preview",
                        color = WaveMuted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF100C16))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { }
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = Color(0xFFFF4D88)
                    )
                }

                IconButton(
                    onClick = { }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { showGifts = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePurple
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Gift"
                    )

                    Spacer(modifier = Modifier.size(6.dp))

                    Text("Gifts")
                }
            }
        }

        if (showGifts) {
            GiftPanel(
                coins = coins,
                onClose = { showGifts = false },
                onGift = {
                    onGiftSent(it)
                    showGifts = false
                }
            )
        }
    }
}

private data class WaveGift(
    val name: String,
    val emoji: String,
    val price: Int
)

@Composable
private fun GiftPanel(
    coins: Int,
    onClose: () -> Unit,
    onGift: (Int) -> Unit
) {
    val gifts = listOf(
        WaveGift("Rose", "🌹", 5),
        WaveGift("Heart", "💖", 20),
        WaveGift("Diamond", "💎", 100),
        WaveGift("Crown", "👑", 500),
        WaveGift("Wave Crown", "👑", 1000)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = 26.dp,
                topEnd = 26.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF17121F)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wave Gifts",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "🪙 $coins",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                gifts.forEach { gift ->
                    GiftRow(
                        gift = gift,
                        enabled = coins >= gift.price,
                        onClick = {
                            onGift(gift.price)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun GiftRow(
    gift: WaveGift,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF211A2B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(WavePurple, WavePink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gift.emoji,
                    fontSize = 29.sp
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = gift.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${gift.price} coins",
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun CreateLiveScreen() {
    var started by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(WavePurple, WavePink)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VideoCall,
                contentDescription = "Create live",
                tint = Color.White,
                modifier = Modifier.size(45.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (started) "Live Ready" else "Start your Live",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (started)
                "Your live session is ready."
            else
                "Create a live room and connect with your audience.",
            color = WaveMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                started = !started
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = WavePurple
            )
        ) {
            Text(
                text = if (started) "Stop Preview" else "Start Live",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (started) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Stream Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "RTMPS URL",
                        color = WaveMuted,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Configure your live server from the Wave backend.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mail,
            contentDescription = "Inbox",
            tint = WavePurple,
            modifier = Modifier.size(70.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "INBOX",
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Notifications and messages will appear here.",
            color = WaveMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileScreen(
    coins: Int,
    onAddCoins: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveDark)
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(WavePurple, WavePink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(70.dp)
                )
            }

            Column(
                modifier = Modifier.padding(start = 15.dp)
            ) {
                Text(
                    text = "Wave User",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "@waveuser",
                    color = WaveMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WaveCard
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = "Wallet",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(34.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Wave Coins",
                        color = WaveMuted
                    )

                    Text(
                        text = coins.toString(),
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Button(
                    onClick = onAddCoins,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePurple
                    )
                ) {
                    Text("+500")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileOption(
            icon = Icons.Default.Wallet,
            title = "Wallet & Coins"
        )

        ProfileOption(
            icon = Icons.Default.CardGiftcard,
            title = "My Gifts"
        )

        ProfileOption(
            icon = Icons.Default.Settings,
            title = "Settings"
        )
    }
}

@Composable
private fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = WavePurple,
                modifier = Modifier.size(25.dp)
            )

            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}
