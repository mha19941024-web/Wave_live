package com.vyro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        setContent { WaveLiveApp() }
    }
}

@Composable
fun WaveLiveApp() {
    var started by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Card, primary = Purple)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (!started) Splash { started = true } else MainScreen()
        }
    }
}

@Composable
fun Splash(onStart: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF05030A), Color(0xFF160B22)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WAVE", fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Wave Live", color = Color.LightGray, fontSize = 18.sp)
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                modifier = Modifier.width(210.dp).height(54.dp)
            ) { Text("Start Live", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

enum class Tab(val title: String, val icon: String) {
    HOME("Home", "⌂"), LIVE("LIVE", "●"), CREATE("Create", "+"), INBOX("Inbox", "✉"), PROFILE("Profile", "●")
}

@Composable
fun MainScreen() {
    var tab by remember { mutableStateOf(Tab.LIVE) }
    var coins by remember { mutableIntStateOf(1200) }
    var showWallet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E0A14)) {
                Tab.values().forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.icon, fontSize = 20.sp) },
                        label = { Text(item.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Purple.copy(alpha = .35f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                Tab.HOME -> HomeScreen()
                Tab.LIVE -> LiveScreen(coins, onWallet = { showWallet = true }, onGift = { if (coins >= it) coins -= it })
                Tab.CREATE -> CreateScreen()
                Tab.INBOX -> CenterMessage("INBOX", "Notifications coming soon")
                Tab.PROFILE -> CenterMessage("PROFILE", "Your Wave profile")
            }
            if (showWallet) WalletDialog(coins, onClose = { showWallet = false }, onAdd = { coins += it })
        }
    }
}

@Composable
fun HomeScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Wave Live", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Discover live creators", color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Text("Trending", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        repeat(4) { LiveCard("Creator ${it + 1}") }
    }
}

@Composable
fun LiveCard(name: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp))
            .background(Card).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Purple, Pink))))
        Spacer(Modifier.width(12.dp))
        Column { Text(name, fontWeight = FontWeight.Bold); Text("LIVE now", color = Pink, fontSize = 12.sp) }
    }
}

@Composable
fun LiveScreen(coins: Int, onWallet: () -> Unit, onGift: (Int) -> Unit) {
    var selectedGift by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color(0xFF100B18))) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("LIVE", color = Pink, fontWeight = FontWeight.Black, fontSize = 14.sp); Text("Wave Creator", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = onWallet) { Text("🪙 $coins") }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF29163D), Color(0xFF0B0810)))),
                contentAlignment = Alignment.Center
            ) { Text("LIVE VIDEO", color = Color.Gray, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(14.dp))
            Text("Send a gift", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            val gifts = listOf("👑" to 100, "💎" to 250, "🚀" to 500, "🌟" to 1000)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(gifts) { (emoji, price) ->
                    Column(
                        Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).background(Card)
                            .clickable { selectedGift = emoji; onGift(price) }.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 34.sp)
                        Text("$price", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
            if (selectedGift != null) {
                Text("Gift sent: $selectedGift", color = Purple, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun CreateScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Go Live", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("Start your Wave live session", color = Color.Gray)
        Spacer(Modifier.height(30.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Text("Start Broadcast")
        }
    }
}

@Composable
fun CenterMessage(title: String, message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.Gray)
        }
    }
}

@Composable
fun WalletDialog(coins: Int, onClose: () -> Unit, onAdd: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Wave Wallet") },
        text = {
            Column {
                Text("Balance: $coins coins", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Wallet payment options")
                Spacer(Modifier.height(8.dp))
                Text("01284306120")
                Text("01144210918")
                Spacer(Modifier.height(14.dp))
                Text("Demo recharge buttons")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAdd(500); onClose() }) { Text("+500") }
                    Button(onClick = { onAdd(1000); onClose() }) { Text("+1000") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}
