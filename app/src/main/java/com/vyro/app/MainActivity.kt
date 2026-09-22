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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
private val WaveGreen = Color(0xFF22C55E)
private val WaveGold = Color(0xFFFFD54F)

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
    var showSplash by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        delay(1500)
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
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                WavePurple,
                                WavePink
                            )
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

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "WAVE",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Wave Live",
                color = WaveMuted,
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "LIVE  •  MUSIC  •  FRIENDS",
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

@Composable
private fun WaveApp() {
    var selectedTab by remember {
        mutableStateOf(WaveTab.HOME)
    }

    var coins by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        containerColor = WaveDark,
        topBar = {
            WaveTopBar(
                selectedTab = selectedTab,
                coins = coins
            )
        },
        bottomBar = {
            WaveBottomBar(
                selectedTab = selectedTab,
                onSelect = {
                    selectedTab = it
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {

                WaveTab.HOME -> {
                    HomeScreen(
                        onOpenLive = {
                            selectedTab = WaveTab.LIVE
                        }
                    )
                }

                WaveTab.LIVE -> {
                    LiveScreen(
                        coins = coins,
                        onGiftSent = { price ->
                            if (coins >= price) {
                                coins -= price
                            }
                        }
                    )
                }

                WaveTab.CREATE -> {
                    CreateLiveScreen()
                }

                WaveTab.INBOX -> {
                    InboxScreen()
                }

                WaveTab.PROFILE -> {
                    ProfileScreen(
                        coins = coins,
                        onAddCoins = {
                            coins += 500
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveTopBar(
    selectedTab: WaveTab,
    coins: Int
) {
    val title = when (selectedTab) {
        WaveTab.HOME -> "WAVE"
        WaveTab.LIVE -> "LIVE"
        WaveTab.CREATE -> "CREATE"
        WaveTab.INBOX -> "INBOX"
        WaveTab.PROFILE -> "PROFILE"
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                color = WaveText,
                fontWeight = FontWeight.ExtraBold
            )
        },
        actions = {
            CoinBadge(
                coins = coins
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = WaveDark
        )
    )
}

@Composable
private fun CoinBadge(
    coins: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF211A2B)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COINS",
                color = WaveGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.width(7.dp)
            )

            Text(
                text = coins.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun WaveBottomBar(
    selectedTab: WaveTab,
    onSelect: (WaveTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color(0xFF100C16)
    ) {
        NavigationBarItem(
            selected = selectedTab == WaveTab.HOME,
            onClick = {
                onSelect(WaveTab.HOME)
            },
            icon = {
                SimpleNavMark(
                    text = "H",
                    selected = selectedTab == WaveTab.HOME
                )
            },
            label = {
                Text("Home")
            }
        )

        NavigationBarItem(
            selected = selectedTab == WaveTab.LIVE,
            onClick = {
                onSelect(WaveTab.LIVE)
            },
            icon = {
                SimpleNavMark(
                    text = "L",
                    selected = selectedTab == WaveTab.LIVE
                )
            },
            label = {
                Text("Live")
            }
        )

        NavigationBarItem(
            selected = selectedTab == WaveTab.CREATE,
            onClick = {
                onSelect(WaveTab.CREATE)
            },
            icon = {
                SimpleNavMark(
                    text = "+",
                    selected = selectedTab == WaveTab.CREATE
                )
            },
            label = {
                Text("Create")
            }
        )

        NavigationBarItem(
            selected = selectedTab == WaveTab.INBOX,
            onClick = {
                onSelect(WaveTab.INBOX)
            },
            icon = {
                SimpleNavMark(
                    text = "M",
                    selected = selectedTab == WaveTab.INBOX
                )
            },
            label = {
                Text("Inbox")
            }
        )

        NavigationBarItem(
            selected = selectedTab == WaveTab.PROFILE,
            onClick = {
                onSelect(WaveTab.PROFILE)
            },
            icon = {
                SimpleNavMark(
                    text = "P",
                    selected = selectedTab == WaveTab.PROFILE
                )
            },
            label = {
                Text("Profile")
            }
        )
    }
}

@Composable
private fun SimpleNavMark(
    text: String,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    WavePurple
                } else {
                    Color(0xFF2A2333)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
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
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Welcome to Wave",
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
            LiveHeroCard(
                onClick = onOpenLive
            )
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
            Spacer(
                modifier = Modifier.height(20.dp)
            )
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
            .clickable {
                onClick()
            },
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
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "GO LIVE",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Start broadcasting on Wave",
                    color = Color.White.copy(
                        alpha = 0.85f
                    ),
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
            .clickable {
                onClick()
            },
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
                            listOf(
                                WavePurple,
                                WavePink
                            )
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

                Text(
                    text = "LIVE NOW",
                    color = WaveGreen,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "OPEN",
                color = WavePurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveScreen(
    coins: Int,
    onGiftSent: (Int) -> Unit
) {
    var showGifts by remember {
        mutableStateOf(false)
    }

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
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            WavePurple,
                                            WavePink
                                        )
                                    )
                                ),
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
                            modifier = Modifier.padding(
                                start = 10.dp
                            )
                        ) {
                            Text(
                                text = "Wave Creator",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "LIVE ROOM",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(
                            Alignment.CenterHorizontally
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Live room",
                        color = WaveMuted,
                        modifier = Modifier.align(
                            Alignment.CenterHorizontally
                        )
                    )

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF100C16)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "LIKE",
                    color = Color(0xFFFF4D88),
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.width(18.dp)
                )

                Text(
                    text = "SHARE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        showGifts = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePurple
                    )
                ) {
                    Text("GIFTS")
                }
            }
        }

        if (showGifts) {
            GiftPanel(
                coins = coins,
                onClose = {
                    showGifts = false
                },
                onGift = { price ->
                    onGiftSent(price)
                    showGifts = false
                }
            )
        }
    }
}

private data class WaveGift(
    val name: String,
    val mark: String,
    val price: Int
)

@Composable
private fun GiftPanel(
    coins: Int,
    onClose: () -> Unit,
    onGift: (Int) -> Unit
) {
    val gifts = listOf(
        WaveGift(
            name = "Rose",
            mark = "R",
            price = 5
        ),
        WaveGift(
            name = "Heart",
            mark = "H",
            price = 10
        ),
        WaveGift(
            name = "Diamond",
            mark = "D",
            price = 250
        ),
        WaveGift(
            name = "Wave Crown",
            mark = "C",
            price = 500
        ),
        WaveGift(
            name = "Wave Rocket",
            mark = "X",
            price = 500
        ),
        WaveGift(
            name = "Lion",
            mark = "L",
            price = 1000
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.70f)
            ),
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
                        text = coins.toString(),
                        color = WaveGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                gifts.forEach { gift ->

                    GiftRow(
                        gift = gift,
                        enabled = coins >= gift.price,
                        onClick = {
                            onGift(gift.price)
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                }

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE")
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
            .clickable(
                enabled = enabled
            ) {
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
                            listOf(
                                WavePurple,
                                WavePink
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gift.mark,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
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
                    color = WaveGold,
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
                Text("SEND")
            }
        }
    }
}

@Composable
private fun CreateLiveScreen() {
    var started by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            WavePurple,
                            WavePink
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "W",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = if (started) {
                "LIVE READY"
            } else {
                "START YOUR LIVE"
            },
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = if (started) {
                "Your live room is ready."
            } else {
                "Create your live room and connect with your audience."
            },
            color = WaveMuted,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

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
                text = if (started) {
                    "STOP PREVIEW"
                } else {
                    "START LIVE"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

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
                        text = "LIVE SESSION",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = "RTMPS",
                        color = WaveMuted,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Wave live server",
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

        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(WavePurple),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            text = "INBOX",
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Messages and notifications will appear here.",
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveDark)
            .padding(18.dp)
    ) {

        item {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    WavePurple,
                                    WavePink
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "W",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(
                    modifier = Modifier.padding(
                        start = 15.dp
                    )
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

            Spacer(
                modifier = Modifier.height(25.dp)
            )

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

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "WAVE COINS",
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

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            ProfileOption(
                mark = "C",
                title = "Wallet & Coins"
            )

            ProfileOption(
                mark = "G",
                title = "My Gifts"
            )

            ProfileOption(
                mark = "M",
                title = "My Videos"
            )

            ProfileOption(
                mark = "S",
                title = "Settings"
            )

            ProfileOption(
                mark = "A",
                title = "Agency"
            )
        }
    }
}

@Composable
private fun ProfileOption(
    mark: String,
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

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                WavePurple,
                                WavePink
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mark,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(
                    start = 14.dp
                )
            )
        }
    }
}
