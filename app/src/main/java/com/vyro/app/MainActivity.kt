package com.vyro.app

import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val WaveDark = Color(0xFF08060D)
private val WaveCard = Color(0xFF15111D)
private val WavePurple = Color(0xFF8B5CF6)
private val WavePink = Color(0xFFEC4899)
private val WaveGold = Color(0xFFFFD54F)
private val WaveGreen = Color(0xFF22C55E)
private val WaveText = Color(0xFFF8F5FF)
private val WaveMuted = Color(0xFFA9A1B5)

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
            WaveApp()
        }
    }
}

@Composable
private fun WaveApp() {

    var selectedTab by remember {
        mutableStateOf(WaveTab.HOME)
    }

    var coins by remember {
        mutableIntStateOf(0)
    }

    var loggedIn by remember {
        mutableStateOf(false)
    }

    var showLogin by remember {
        mutableStateOf(false)
    }

    /*
     * مهم:
     * LocalContext.current يتم استدعاؤه هنا داخل Composable
     * ثم نستخدم قيمة context داخل LaunchedEffect.
     */
    val context = LocalContext.current

    LaunchedEffect(Unit) {

        delay(900)

        val preferences = context.getSharedPreferences(
            "vyro_session",
            Context.MODE_PRIVATE
        )

        loggedIn = !preferences
            .getString("session", null)
            .isNullOrBlank()
    }

    if (showLogin) {

        LoginScreen(
            onLoginSuccess = {
                loggedIn = true
                showLogin = false
                selectedTab = WaveTab.PROFILE
            },
            onBack = {
                showLogin = false
            }
        )

        return
    }

    Scaffold(
        containerColor = WaveDark,

        bottomBar = {

            NavigationBar(
                containerColor = Color(0xFF100C16)
            ) {

                NavigationBarItem(
                    selected = selectedTab == WaveTab.HOME,
                    onClick = {
                        selectedTab = WaveTab.HOME
                    },
                    icon = {
                        Text(
                            text = "H",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    label = {
                        Text("Home")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.LIVE,
                    onClick = {
                        selectedTab = WaveTab.LIVE
                    },
                    icon = {
                        Text(
                            text = "L",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    label = {
                        Text("Live")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.CREATE,
                    onClick = {
                        selectedTab = WaveTab.CREATE
                    },
                    icon = {
                        Text(
                            text = "+",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    label = {
                        Text("Create")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.INBOX,
                    onClick = {
                        selectedTab = WaveTab.INBOX
                    },
                    icon = {
                        Text(
                            text = "M",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    label = {
                        Text("Inbox")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == WaveTab.PROFILE,
                    onClick = {
                        selectedTab = WaveTab.PROFILE
                    },
                    icon = {
                        Text(
                            text = "P",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    label = {
                        Text("Profile")
                    }
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(WaveDark)
        ) {

            WaveHeader(
                title = when (selectedTab) {
                    WaveTab.HOME -> "WAVE"
                    WaveTab.LIVE -> "LIVE"
                    WaveTab.CREATE -> "CREATE"
                    WaveTab.INBOX -> "INBOX"
                    WaveTab.PROFILE -> "PROFILE"
                },
                coins = coins
            )

            when (selectedTab) {

                WaveTab.HOME -> {
                    HomeScreen(
                        onLive = {
                            selectedTab = WaveTab.LIVE
                        }
                    )
                }

                WaveTab.LIVE -> {
                    LiveScreen(
                        coins = coins,
                        onGift = { price ->
                            if (coins >= price) {
                                coins -= price
                            }
                        }
                    )
                }

                WaveTab.CREATE -> {
                    CreateScreen(
                        onRequireLogin = {
                            showLogin = true
                        }
                    )
                }

                WaveTab.INBOX -> {
                    InboxScreen(
                        loggedIn = loggedIn,
                        onLogin = {
                            showLogin = true
                        }
                    )
                }

                WaveTab.PROFILE -> {
                    ProfileScreen(
                        loggedIn = loggedIn,
                        coins = coins,
                        onLogin = {
                            showLogin = true
                        },
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
private fun WaveHeader(
    title: String,
    coins: Int
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            color = WaveText,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF211A2B)
            )
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
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
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onLive: () -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Welcome to Wave",
                color = WaveText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Live • Music • Friends",
                color = WaveMuted,
                fontSize = 14.sp
            )
        }

        item {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clickable {
                        onLive()
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

                        Text(
                            text = "GO LIVE",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "Join live creators on Wave",
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
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
                "Wave Creator",
                "Music Live",
                "Gaming Live",
                "Chat Live"
            )
        ) { name ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clickable {
                        onLive()
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
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
                            text = name.first().toString(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = name,
                            color = WaveText,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "LIVE NOW",
                            color = WaveGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveScreen(
    coins: Int,
    onGift: (Int) -> Unit
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
                    .height(0.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF27113A),
                                Color.Black
                            )
                        )
                    )
                    .padding(18.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(48.dp)
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
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column {

                        Text(
                            text = "Wave Creator",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "LIVE ROOM",
                            color = WaveGreen,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Live streaming",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Real-time creator room",
                    color = WaveMuted
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Stream status",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Ready",
                            color = WaveGreen,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF100C16))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = {
                        showGifts = !showGifts
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePurple
                    )
                ) {
                    Text("GIFTS")
                }

                OutlinedButton(
                    onClick = {}
                ) {
                    Text("MUSIC")
                }
            }

            if (showGifts) {

                GiftPanel(
                    coins = coins,
                    onGift = onGift
                )
            }
        }
    }
}

@Composable
private fun GiftPanel(
    coins: Int,
    onGift: (Int) -> Unit
) {

    val gifts = listOf(
        "Rose" to 5,
        "Heart" to 20,
        "Fire" to 50,
        "Diamond" to 100,
        "Wave Crown" to 250,
        "Royal Crown" to 500
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF17121F))
            .padding(12.dp)
    ) {

        Text(
            text = "Send a Gift",
            color = WaveText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        gifts.forEach { gift ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = gift.first,
                    color = Color.White
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "${gift.second} coins",
                    color = WaveGold,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Button(
                    onClick = {
                        if (coins >= gift.second) {
                            onGift(gift.second)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WavePurple
                    )
                ) {
                    Text("SEND")
                }
            }
        }
    }
}

@Composable
private fun CreateScreen(
    onRequireLogin: () -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {

            Text(
                text = "Create",
                color = WaveText,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Create videos or start your live",
                color = WaveMuted
            )
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                ),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Video",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Music • Filters • Effects",
                        color = WaveMuted
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onRequireLogin()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WavePurple
                        )
                    ) {
                        Text("CREATE VIDEO")
                    }
                }
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                ),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Live",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Start a real creator session",
                        color = WaveMuted
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onRequireLogin()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WavePink
                        )
                    ) {
                        Text("START LIVE")
                    }
                }
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                ),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Music Library",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Choose music for your creation",
                        color = WaveMuted
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = "Free music sources can be connected through the Wave API.",
                        color = WaveMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxScreen(
    loggedIn: Boolean,
    onLogin: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Text(
            text = "INBOX",
            color = WaveText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (!loggedIn) {

            Text(
                text = "Login to view your messages and notifications.",
                color = WaveMuted,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Button(
                onClick = onLogin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePurple
                )
            ) {
                Text("LOGIN")
            }

        } else {

            Text(
                text = "Your messages and notifications will appear here.",
                color = WaveMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileScreen(
    loggedIn: Boolean,
    coins: Int,
    onLogin: () -> Unit,
    onAddCoins: () -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {

            Text(
                text = "Profile",
                color = WaveText,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                ),
                shape = RoundedCornerShape(22.dp)
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(78.dp)
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    if (loggedIn) {

                        Text(
                            text = "Wave User",
                            color = WaveText,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Verified Wave account",
                            color = WaveMuted
                        )

                    } else {

                        Text(
                            text = "Guest Account",
                            color = WaveText,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Login to create your real account.",
                            color = WaveMuted
                        )
                    }
                }
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                ),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Creator Wallet",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "$coins Wave Coins",
                        color = WaveGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (loggedIn) {
                                onAddCoins()
                            } else {
                                onLogin()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WavePurple
                        )
                    ) {

                        Text(
                            text = if (loggedIn) {
                                "ADD COINS"
                            } else {
                                "LOGIN"
                            }
                        )
                    }
                }
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WaveCard
                ),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Creator Earnings",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Your creator earnings and withdrawals will be managed through the Wave backend.",
                        color = WaveMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    /*
     * مهم:
     * LocalContext.current هنا داخل Composable فقط.
     * لا يتم استدعاؤه داخل onClick كـ Composable invocation.
     */
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WaveDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(50.dp)
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
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Welcome to Wave",
            color = WaveText,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "Sign in to your Wave account",
            color = WaveMuted
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Username or Email")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {

                if (
                    username.isNotBlank() &&
                    password.isNotBlank()
                ) {

                    /*
                     * context تم الحصول عليه مسبقًا من
                     * LocalContext.current داخل Composable.
                     */
                    context
                        .getSharedPreferences(
                            "vyro_session",
                            Context.MODE_PRIVATE
                        )
                        .edit()
                        .putString(
                            "session",
                            username.trim()
                        )
                        .apply()

                    onLoginSuccess()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = WavePurple
            )
        ) {
            Text("LOGIN")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        TextButton(
            onClick = onBack
        ) {

            Text(
                text = "BACK",
                color = WaveMuted
            )
        }
    }
}
