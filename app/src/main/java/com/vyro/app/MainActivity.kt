package com.vyro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WaveBackground = Color(0xFF08060D)
private val WaveCard = Color(0xFF15111D)
private val WavePurple = Color(0xFF9B5CFF)
private val WavePink = Color(0xFFFF4FA3)
private val WaveCyan = Color(0xFF35D9FF)
private val WaveGold = Color(0xFFFFC857)

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

    var started by remember {
        mutableStateOf(false)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = WaveBackground,
            surface = WaveCard,
            primary = WavePurple
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WaveBackground
        ) {
            if (!started) {
                SplashScreen {
                    started = true
                }
            } else {
                MainScreen()
            }
        }
    }
}

@Composable
fun WaveLogo(
    modifier: Modifier = Modifier,
    large: Boolean = false
) {

    val size = if (large) 100.dp else 58.dp
    val textSize = if (large) 42.sp else 25.sp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (large) 28.dp else 18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        WavePurple,
                        WavePink,
                        WaveCyan
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "W",
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun SplashScreen(
    onStart: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05030A),
                        Color(0xFF160B22),
                        Color(0xFF090611)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            WaveLogo(large = true)

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Text(
                text = "WAVE",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Wave Live",
                fontSize = 18.sp,
                color = Color.LightGray
            )

            Spacer(
                modifier = Modifier.height(38.dp)
            )

            Button(
                onClick = onStart,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {
                Text(
                    text = "Start Live",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

enum class WaveTab(
    val title: String,
    val icon: String
) {
    HOME("Home", "⌂"),
    LIVE("LIVE", "●"),
    CREATE("Create", "+"),
    MUSIC("Music", "♫"),
    INBOX("Inbox", "✉"),
    PROFILE("Profile", "●")
}

@Composable
fun MainScreen() {

    var selectedTab by remember {
        mutableStateOf(WaveTab.LIVE)
    }

    var coins by remember {
        mutableIntStateOf(1200)
    }

    var showWallet by remember {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = WaveBackground,

        bottomBar = {

            NavigationBar(
                containerColor = Color(0xFF0E0A14)
            ) {

                WaveTab.values().forEach { item ->

                    NavigationBarItem(
                        selected = selectedTab == item,

                        onClick = {
                            selectedTab = item
                        },

                        icon = {
                            Text(
                                text = item.icon,
                                fontSize = 20.sp
                            )
                        },

                        label = {
                            Text(
                                text = item.title,
                                fontSize = 10.sp
                            )
                        },

                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = WavePurple.copy(
                                alpha = 0.35f
                            ),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when (selectedTab) {

                WaveTab.HOME -> {
                    HomeScreen()
                }

                WaveTab.LIVE -> {
                    LiveScreen(
                        coins = coins,
                        onWallet = {
                            showWallet = true
                        },
                        onGift = { price ->
                            if (coins >= price) {
                                coins -= price
                            }
                        }
                    )
                }

                WaveTab.CREATE -> {
                    CreateScreen()
                }

                WaveTab.MUSIC -> {
                    MusicScreen()
                }

                WaveTab.INBOX -> {
                    InboxScreen()
                }

                WaveTab.PROFILE -> {
                    ProfileScreen(
                        coins = coins,
                        onWallet = {
                            showWallet = true
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
                        coins += amount
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            WaveLogo()

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Wave Live",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Discover live creators",
                    color = Color.Gray
                )
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "LIVE NOW",
            color = WavePink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        repeat(4) { index ->

            LiveCreatorCard(
                name = "Wave Creator ${index + 1}"
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Featured Music",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        MusicMiniCard(
            title = "Wave Night",
            artist = "Wave Music"
        )
    }
}

@Composable
fun LiveCreatorCard(
    name: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(WaveCard)
            .padding(14.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(52.dp)
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
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = name,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "LIVE now",
                color = WavePink,
                fontSize = 12.sp
            )
        }

        Text(
            text = "●",
            color = WavePink,
            fontSize = 16.sp
        )
    }
}

@Composable
fun LiveScreen(
    coins: Int,
    onWallet: () -> Unit,
    onGift: (Int) -> Unit
) {

    var selectedFilter by remember {
        mutableStateOf("None")
    }

    var selectedGift by remember {
        mutableStateOf<String?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100B18))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    WaveLogo()

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column {

                        Text(
                            text = "LIVE",
                            color = WavePink,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Wave Creator",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onWallet
                ) {

                    Text(
                        text = "🪙 $coins"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
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
                                Color(0xFF0B0810),
                                Color(0xFF162A3A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    WaveLogo(
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "LIVE VIDEO",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Filter: $selectedFilter",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Filters",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(
                    rememberScrollState()
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "None",
                    "Neon",
                    "Warm",
                    "Dream",
                    "Wave"
                ).forEach { filter ->

                    Button(
                        onClick = {
                            selectedFilter = filter
                        },

                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (selectedFilter == filter)
                                    WavePurple
                                else
                                    WaveCard
                        )
                    ) {

                        Text(filter)
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Send a gift",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            val gifts = listOf(
                GiftItem(
                    "rose",
                    "Rose",
                    "🌹",
                    5
                ),

                GiftItem(
                    "heart",
                    "Heart",
                    "💖",
                    10
                ),

                GiftItem(
                    "crown",
                    "Wave Crown",
                    "👑",
                    500
                ),

                GiftItem(
                    "diamond",
                    "Diamond",
                    "💎",
                    250
                ),

                GiftItem(
                    "rocket",
                    "Rocket",
                    "🚀",
                    500
                ),

                GiftItem(
                    "star",
                    "Super Star",
                    "🌟",
                    1000
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                gifts.forEach { gift ->

                    GiftCard(
                        gift = gift,

                        enabled = coins >= gift.price,

                        onClick = {
                            selectedGift = gift.name
                            onGift(gift.price)
                        }
                    )
                }
            }

            if (selectedGift != null) {

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text = "Gift sent: $selectedGift",
                    color = WavePurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class GiftItem(
    val id: String,
    val name: String,
    val icon: String,
    val price: Int
)

@Composable
fun GiftCard(
    gift: GiftItem,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled)
                    WaveCard
                else
                    Color(0xFF0F0C13)
            )
            .clickable(enabled = enabled) {
                onClick()
            }
            .padding(10.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = gift.icon,
            fontSize = 34.sp
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = gift.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "🪙 ${gift.price}",
            fontSize = 11.sp,
            color = WaveGold
        )
    }
}

@Composable
fun CreateScreen() {

    var liveTitle by remember {
        mutableStateOf("My Wave Live")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp)
    ) {

        Text(
            text = "Go Live",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Create your Wave live session",
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "Live title",
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        androidx.compose.material3.OutlinedTextField(
            value = liveTitle,
            onValueChange = {
                liveTitle = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Title")
            }
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WavePurple
            )
        ) {

            Text(
                text = "Start Broadcast",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Text(
            text = "Your live session can receive Wave gifts.",
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

@Composable
fun MusicScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            WaveLogo()

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Wave Music",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Music for your Wave",
                    color = Color.Gray
                )
            }
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Text(
            text = "Featured",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        MusicMiniCard(
            title = "Wave Night",
            artist = "Wave Music"
        )

        MusicMiniCard(
            title = "Live Energy",
            artist = "Wave Sounds"
        )

        MusicMiniCard(
            title = "Dream Wave",
            artist = "Wave Studio"
        )

        MusicMiniCard(
            title = "Creator Beat",
            artist = "Wave Originals"
        )
    }
}

@Composable
fun MusicMiniCard(
    title: String,
    artist: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(WaveCard)
            .padding(14.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            WavePurple,
                            WaveCyan
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "♫",
                fontSize = 27.sp,
                color = Color.White
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = artist,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Text(
            text = "▶",
            color = WavePurple,
            fontSize = 20.sp
        )
    }
}

@Composable
fun InboxScreen() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "✉",
                fontSize = 50.sp,
                color = WavePurple
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "INBOX",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Notifications coming soon",
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ProfileScreen(
    coins: Int,
    onWallet: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        WaveLogo(
            large = true
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Text(
            text = "Wave Creator",
            fontSize = 27.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "@wavecreator",
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            ProfileStat(
                value = "0",
                label = "Followers"
            )

            ProfileStat(
                value = "0",
                label = "Following"
            )

            ProfileStat(
                value = "0",
                label = "Lives"
            )
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Button(
            onClick = onWallet,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WavePurple
            ),
            shape = RoundedCornerShape(27.dp)
        ) {

            Text(
                text = "🪙 Wave Wallet: $coins",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileStat(
    value: String,
    label: String
) {

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(WaveCard)
            .padding(12.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun WalletDialog(
    coins: Int,
    onClose: () -> Unit,
    onAdd: (Int) -> Unit
) {

    AlertDialog(

        onDismissRequest = onClose,

        title = {

            Text(
                text = "Wave Wallet",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column {

                Text(
                    text = "Current balance",
                    color = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "🪙 $coins Wave Coins",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = WaveGold
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Wallet payment options",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "01284306120",
                    fontSize = 16.sp
                )

                Text(
                    text = "01144210918",
                    fontSize = 16.sp
                )

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                Text(
                    text = "Recharge packages",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            onAdd(500)
                            onClose()
                        }
                    ) {
                        Text("+500")
                    }

                    Button(
                        onClick = {
                            onAdd(1000)
                            onClose()
                        }
                    ) {
                        Text("+1000")
                    }
                }

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text = "Demo recharge — connect payment API before production.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        },

        confirmButton = {

            TextButton(
                onClick = onClose
            ) {
                Text("Close")
            }
        }
    )
}
