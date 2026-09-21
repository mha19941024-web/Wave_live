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
import androidx.compose.foundation.layout.weight
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
                           
