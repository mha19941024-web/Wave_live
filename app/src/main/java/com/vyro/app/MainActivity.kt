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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
private val WaveSurface = Color(0xFF15121C)
private val WavePurple = Color(0xFF9B5CFF)
private val WavePink = Color(0xFFFF4FA3)
private val WaveGreen = Color(0xFF20D889)
private val WaveGold = Color(0xFFFFC857)

private val WaveScheme = darkColorScheme(
    primary = WavePurple,
    secondary = WavePink,
    background = WaveBackground,
    surface = WaveSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = WaveScheme
            ) {
                WaveLive()
            }
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

private data class Creator(
    val name: String,
    val username: String,
    val viewers: String
)

private data class Gift(
    val name: String,
    val icon: String,
    val price: Int
)

@Composable
private fun WaveLive() {

    var loggedIn by remember {
        mutableStateOf(false)
    }

    if (!loggedIn) {
        LoginScreen(
            onLogin = {
                loggedIn = true
            }
        )
    } else {
        MainScreen()
    }
}

@Composable
private fun LoginScreen(
    onLogin: () -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var register by remember {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "WAVE",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "WAVE LIVE",
                color = WavePurple,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = if (register)
                    "إنشاء حساب جديد"
                else
                    "تسجيل الدخول إلى حسابك",
                color = Color.LightGray
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("البريد الإلكتروني")
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
                    Text("كلمة المرور")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(20.dp)
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
                    if (register)
                        "إنشاء الحساب"
                    else
                        "تسجيل الدخول"
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            TextButton(
                onClick = {
                    register = !register
                }
            ) {

                Text(
                    if (register)
                        "لدي حساب بالفعل"
                    else
                        "إنشاء حساب جديد"
                )
            }
        }
    }
}

@Composable
private fun MainScreen() {

    var selectedTab by remember {
        mutableStateOf(Tab.HOME)
    }

    var showWallet by remember {
        mutableStateOf(false)
    }

    var showLive by remember {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = WaveBackground,

        bottomBar = {

            NavigationBar(
                containerColor = WaveSurface
            ) {

                NavigationBarItem(
                    selected = selectedTab == Tab.HOME,
                    onClick = {
                        selectedTab = Tab.HOME
                    },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("الرئيسية")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == Tab.LIVE,
                    onClick = {
                        selectedTab = Tab.LIVE
                    },
                    icon = {
                        Icon(
                            Icons.Default.LiveTv,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("مباشر")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == Tab.CREATE,
                    onClick = {
                        selectedTab = Tab.CREATE
                    },
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("إنشاء")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == Tab.INBOX,
                    onClick = {
                        selectedTab = Tab.INBOX
                    },
                    icon = {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("الوارد")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == Tab.PROFILE,
                    onClick = {
                        selectedTab = Tab.PROFILE
                    },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("حسابي")
                    }
                )
            }
        }
    ) { padding ->

        when (selectedTab) {

            Tab.HOME -> HomeScreen(
                modifier = Modifier.padding(padding),
                onWallet = {
                    showWallet = true
                },
                onLive = {
                    selectedTab = Tab.LIVE
                }
            )

            Tab.LIVE -> LiveScreen(
                modifier = Modifier.padding(padding),
                onWallet = {
                    showWallet = true
                },
                onOpenLive = {
                    showLive = true
                }
            )

            Tab.CREATE -> CreateScreen(
                modifier = Modifier.padding(padding),
                onWallet = {
                    showWallet = true
                },
                onStartLive = {
                    showLive = true
                }
            )

            Tab.INBOX -> InboxScreen(
                modifier = Modifier.padding(padding)
            )

            Tab.PROFILE -> ProfileScreen(
                modifier = Modifier.padding(padding),
                onWallet = {
                    showWallet = true
                }
            )
        }
    }

    if (showWallet) {

        WalletScreen(
            onClose = {
                showWallet = false
            }
        )
    }

    if (showLive) {

        LiveRoom(
            onClose = {
                showLive = false
            }
        )
    }
}

@Composable
private fun Header(
    title: String,
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

        IconButton(
            onClick = onWallet
        ) {

            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = "المحفظة",
                tint = WaveGold
            )
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    onWallet: () -> Unit,
    onLive: () -> Unit
) {

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
    ) {

        item {

            Header(
                title = "WAVE",
                onWallet = onWallet
            )

            Text(
                text = "اكتشف البث والفيديوهات",
                color = Color.LightGray,
                modifier = Modifier.padding(
                    horizontal = 16.dp
                )
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onLive,
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

                Text("شاهد البث المباشر")
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "الفيديوهات",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = 16.dp
                )
            )
        }

        items(
            listOf(
                "أول فيديو على Wave Live",
                "استمتع بالمحتوى الجديد",
                "اكتشف مبدعين جدد"
            )
        ) { title ->

            VideoCard(title)
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
            .padding(
                horizontal = 16.dp,
                vertical = 7.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {

        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(Color(0xFF211A2D)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Default.PlayArrow,
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

                Avatar("W")

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Wave Creator",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = title,
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }

                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = WavePink
                )
            }
        }
    }
}

@Composable
private fun LiveScreen(
    modifier: Modifier,
    onWallet: () -> Unit,
    onOpenLive: () -> Unit
) {

    val creators = listOf(
        Creator(
            "Wave Star",
            "@wave_star",
            "2.4K"
        ),
        Creator(
            "Mona Wave",
            "@mona_wave",
            "1.8K"
        ),
        Creator(
            "Ahmed Live",
            "@ahmed_live",
            "850"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
    ) {

        item {

            Header(
                title = "البث المباشر",
                onWallet = onWallet
            )

            Button(
                onClick = onOpenLive,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WavePink
                )
            ) {

                Text("ابدأ بث مباشر")
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        items(creators) { creator ->

            CreatorCard(
                creator = creator,
                onClick = onOpenLive
            )
        }
    }
}

@Composable
private fun CreatorCard(
    creator: Creator,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 7.dp
            )
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Avatar(
                creator.name.take(1)
            )

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
                    text = creator.username,
                    color = Color.Gray
                )

                Text(
                    text = "🔴 ${creator.viewers} يشاهدون الآن",
                    color = WavePink,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onClick,
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
    onWallet: () -> Unit,
    onStartLive: () -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
            .padding(16.dp)
    ) {

        Header(
            title = "إنشاء",
            onWallet = onWallet
        )

        ActionCard(
            icon = "🔴",
            title = "بث مباشر",
            description = "ابدأ غرفة بث وتفاعل مع جمهورك.",
            button = "ابدأ الآن",
            onClick = onStartLive
        )

        ActionCard(
            icon = "🎬",
            title = "رفع فيديو",
            description = "اختر فيديو من هاتفك لرفعه إلى Wave.",
            button = "اختيار فيديو",
            onClick = {}
        )

        ActionCard(
            icon = "💰",
            title = "أرباح المبدعين",
            description = "تابع أرباحك من المحتوى والهدايا.",
            button = "المحفظة",
            onClick = onWallet
        )
    }
}

@Composable
private fun ActionCard(
    icon: String,
    title: String,
    description: String,
    button: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaveSurface
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

            Text(
                text = title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
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

                Text(button)
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
            modifier = Modifier.height(30.dp)
        )

        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            tint = WavePurple,
            modifier = Modifier.size(64.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "الوارد",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "الرسائل والإشعارات ستظهر هنا.",
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    onWallet: () -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
            .padding(16.dp)
    ) {

        Header(
            title = "حسابي",
            onWallet = onWallet
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WaveSurface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Avatar(
                    letter = "W",
                    size = 88
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "مستخدم Wave",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "@wave_user",
                    color = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    Stat("المتابعون", "0")
                    Stat("الفيديوهات", "0")
                    Stat("الإعجابات", "0")
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Button(
                    onClick = onWallet,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WaveGreen
                    )
                ) {

                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("المحفظة والأرباح")
                }
            }
        }
    }
}

@Composable
private fun Stat(
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
private fun Avatar(
    letter: String,
    size: Int = 54
) {

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(WavePurple),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = letter,
            color = Color.White,
            fontSize = (size / 2).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WalletScreen(
    onClose: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onClose,

        title = {
            Text("محفظة Wave")
        },

        text = {

            Column {

                Text(
                    text = "🪙 العملات",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "رصيد العملات سيُقرأ من حسابك عند ربط الخادم."
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = "طرق الدفع المتاحة"
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text("محافظ الدفع فقط")

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "01284306120",
                    color = WaveGreen,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "01144210918",
                    color = WaveGreen,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "بعد التحويل يتم تسجيل طلب الشحن ومراجعته قبل إضافة العملات."
                )
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
private fun LiveRoom(
    onClose: () -> Unit
) {

    val gifts = listOf(
        Gift(
            "تاج Wave",
            "👑",
            100
        ),
        Gift(
            "قلب ماسي",
            "💎",
            250
        ),
        Gift(
            "صاروخ Wave",
            "🚀",
            500
        ),
        Gift(
            "قلعة Wave",
            "🏰",
            1000
        )
    )

    AlertDialog(
        onDismissRequest = onClose,

        title = {
            Text("Wave Live")
        },

        text = {

            Column {

                Text(
                    text = "غرفة البث المباشر",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "الهدايا"
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
                            onClick = {}
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
