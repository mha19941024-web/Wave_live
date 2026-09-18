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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WaveGreen = Color(0xFF19D36B)
private val Dark = Color(0xFF0B0F0D)
private val CardColor = Color(0xFF151B18)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaveLiveApp()
        }
    }
}

private enum class Tab {
    HOME,
    LIVE,
    CREATE,
    INBOX,
    PROFILE
}

@Composable
private fun WaveLiveApp() {

    var started by remember {
        mutableStateOf(false)
    }

    var selectedTab by remember {
        mutableStateOf(Tab.HOME)
    }

    var coins by remember {
        mutableIntStateOf(1250)
    }

    var earnings by remember {
        mutableStateOf(0.0)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = WaveGreen,
            background = Dark,
            surface = CardColor,
            onPrimary = Color.Black
        )
    ) {

        if (!started) {

            SplashScreen(
                onStart = {
                    started = true
                }
            )

        } else {

            Scaffold(
                containerColor = Dark,

                bottomBar = {

                    NavigationBar(
                        containerColor = Color(0xFF101512)
                    ) {

                        NavigationItem(
                            label = "الرئيسية",
                            icon = Icons.Default.Home,
                            selected = selectedTab == Tab.HOME
                        ) {
                            selectedTab = Tab.HOME
                        }

                        NavigationItem(
                            label = "LIVE",
                            icon = Icons.Default.Videocam,
                            selected = selectedTab == Tab.LIVE
                        ) {
                            selectedTab = Tab.LIVE
                        }

                        NavigationItem(
                            label = "إنشاء",
                            icon = Icons.Default.AddCircle,
                            selected = selectedTab == Tab.CREATE
                        ) {
                            selectedTab = Tab.CREATE
                        }

                        NavigationItem(
                            label = "الوارد",
                            icon = Icons.Default.Notifications,
                            selected = selectedTab == Tab.INBOX
                        ) {
                            selectedTab = Tab.INBOX
                        }

                        NavigationItem(
                            label = "لك",
                            icon = Icons.Default.Person,
                            selected = selectedTab == Tab.PROFILE
                        ) {
                            selectedTab = Tab.PROFILE
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

                        Tab.HOME -> {
                            HomeScreen(coins)
                        }

                        Tab.LIVE -> {
                            LiveScreen()
                        }

                        Tab.CREATE -> {
                            CreateScreen(
                                onCreatorReward = {
                                    earnings += 5.0
                                }
                            )
                        }

                        Tab.INBOX -> {
                            InboxScreen()
                        }

                        Tab.PROFILE -> {
                            ProfileScreen(
                                coins = coins,
                                earnings = earnings,
                                onAddCoins = {
                                    coins += 500
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(
    onStart: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark)
            .padding(28.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "WAVE",
            color = WaveGreen,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Wave Live",
            color = Color.White,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(34.dp)
        )

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {

            Text(
                text = "ابدأ الآن",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {

    NavigationBarItem(

        selected = selected,

        onClick = onClick,

        icon = {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = label
            )
        },

        label = {
            Text(
                text = label,
                fontSize = 11.sp
            )
        },

        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = WaveGreen,
            selectedTextColor = WaveGreen,
            unselectedIconColor = Color.LightGray,
            unselectedTextColor = Color.LightGray,
            indicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun HomeScreen(
    coins: Int
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),

        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "Wave Live",
                        color = WaveGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "شاهد، تفاعل، واصنع محتواك",
                        color = Color.LightGray
                    )
                }

                AssistChip(
                    onClick = {},
                    label = {
                        Text("🪙 $coins")
                    }
                )
            }
        }

        item {

            Text(
                text = "مباشر الآن",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    listOf(
                        "سارة",
                        "محمد",
                        "نور",
                        "أحمد"
                    )
                ) { name ->

                    LiveCard(name)
                }
            }
        }

        item {

            Text(
                text = "فيديوهات مقترحة",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            listOf(
                "فيديو جديد من Wave",
                "رحلة اليوم",
                "تحدي Wave",
                "محتوى المبدعين"
            )
        ) { title ->

            VideoCard(title)
        }
    }
}

@Composable
private fun LiveCard(
    name: String
) {

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(120.dp),

        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF202722)),

            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            WaveGreen,
                            CircleShape
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = name.take(1),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "🔴 LIVE",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun VideoCard(
    title: String
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),

        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Color(0xFF202722),
                        RoundedCornerShape(14.dp)
                    ),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "▶",
                    color = WaveGreen,
                    fontSize = 42.sp
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Text(
            text = "البث المباشر",
            color = WaveGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        ActionCard(
            icon = "🔴",
            title = "ابدأ بثك الآن",
            subtitle = "شارك جمهورك وابدأ في بناء مجتمعك على Wave Live"
        ) {}

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "البثوث المقترحة",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        LiveCard("Wave")
    }
}

@Composable
private fun CreateScreen(
    onCreatorReward: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Text(
            text = "إنشاء",
            color = WaveGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        ActionCard(
            icon = "📹",
            title = "رفع فيديو",
            subtitle = "اختر فيديو من جهازك وانشره على Wave Live"
        ) {}

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ActionCard(
            icon = "🔴",
            title = "بدء بث مباشر",
            subtitle = "ابدأ بثاً جديداً وتفاعل مع المتابعين"
        ) {}

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ActionCard(
            icon = "💰",
            title = "مركز المبدع",
            subtitle = "تابع أرباحك ومكافآتك"
        ) {
            onCreatorReward()
        }
    }
}

@Composable
private fun ActionCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {

        Row(
            modifier = Modifier.padding(18.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = icon,
                fontSize = 34.sp
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = Color.LightGray
                )
            }

            androidx.compose.material3.Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = WaveGreen
            )
        }
    }
}

@Composable
private fun InboxScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Text(
            text = "الوارد",
            color = WaveGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        ActionCard(
            icon = "🔔",
            title = "الإشعارات",
            subtitle = "إشعارات المتابعين والبثوث والهدايا"
        ) {}

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ActionCard(
            icon = "🎁",
            title = "الهدايا",
            subtitle = "تابع الهدايا التي استلمتها أثناء البث"
        ) {}
    }
}

@Composable
private fun ProfileScreen(
    coins: Int,
    earnings: Double,
    onAddCoins: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        WaveGreen,
                        CircleShape
                    ),

                contentAlignment = Alignment.Center
            ) {

                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column {

                Text(
                    text = "مستخدم Wave",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "@wave_creator",
                    color = Color.LightGray
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StatCard(
                icon = "🪙",
                value = coins.toString(),
                label = "Coins",
                modifier = Modifier.weight(1f)
            )

            StatCard(
                icon = "💰",
                value = String.format("%.2f", earnings),
                label = "الأرباح",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        ActionCard(
            icon = "👤",
            title = "إضافة / تعديل الحساب",
            subtitle = "أكمل بيانات ملفك الشخصي"
        ) {}

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ActionCard(
            icon = "🪙",
            title = "شحن Wave Coins",
            subtitle = "إضافة رصيد Coins"
        ) {
            onAddCoins()
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ActionCard(
            icon = "💳",
            title = "محفظة المبدع",
            subtitle = "الأرباح وطلبات السحب"
        ) {}
    }
}

@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier
) {

    Card(
        modifier = modifier.height(105.dp),

        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "$icon $value",
                color = WaveGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                color = Color.LightGray
            )
        }
    }
}
