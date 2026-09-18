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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

class MainActivity : ComponentActivity() {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
        MaterialTheme {
            WaveApp()
        }
    }
}

}

@androidx.compose.runtime.Composable
fun WaveApp() {

var selectedTab by remember {
    mutableIntStateOf(0)
}

val tabs = listOf(
    "Home",
    "LIVE",
    "Create",
    "Inbox",
    "Profile"
)

Scaffold(
    containerColor = Color(0xFF08080D),
    bottomBar = {
        NavigationBar(
            containerColor = Color(0xFF111116)
        ) {
            tabs.forEachIndexed { index, title ->

                val icon = when (index) {
                    0 -> Icons.Default.Home
                    1 -> Icons.Default.PlayArrow
                    2 -> Icons.Default.Add
                    3 -> Icons.Default.Mail
                    else -> Icons.Default.Person
                }

                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = title
                        )
                    },
                    label = {
                        Text(title)
                    }
                )
            }
        }
    }
) { padding ->

    when (selectedTab) {

        0 -> HomeScreen(
            modifier = Modifier.padding(padding)
        )

        1 -> LiveScreen(
            modifier = Modifier.padding(padding)
        )

        2 -> CreateScreen(
            modifier = Modifier.padding(padding)
        )

        3 -> InboxScreen(
            modifier = Modifier.padding(padding)
        )

        4 -> ProfileScreen(
            modifier = Modifier.padding(padding)
        )
    }
}

}

@androidx.compose.runtime.Composable
fun HomeScreen(
modifier: Modifier = Modifier
) {

Column(
    modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF08080D))
        .padding(16.dp)
) {

    Text(
        text = "WAVE",
        color = Color.White,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold
    )

    Text(
        text = "Wave Live",
        color = Color(0xFFB98CFF),
        fontSize = 15.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Live Now",
        color = Color.White,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        items(
            listOf(
                "LIVE 01",
                "LIVE 02",
                "LIVE 03",
                "LIVE 04"
            )
        ) { title ->

            LiveCard(title)
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = "Popular Rooms",
        color = Color.White,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    repeat(3) { index ->

        RoomItem(
            title = "Wave Room ${index + 1}",
            viewers = "${(index + 1) * 125} viewers"
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

}

@androidx.compose.runtime.Composable
fun LiveCard(
title: String
) {

Box(
    modifier = Modifier
        .size(width = 145.dp, height = 190.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF7A3FFF),
                    Color(0xFF15151D)
                )
            )
        )
        .clickable { },
    contentAlignment = Alignment.BottomStart
) {

    Column(
        modifier = Modifier.padding(12.dp)
    ) {

        Text(
            text = "● LIVE",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

}

@androidx.compose.runtime.Composable
fun RoomItem(
title: String,
viewers: String
) {

Surface(
    modifier = Modifier.fillMaxWidth(),
    color = Color(0xFF15151D),
    shape = RoundedCornerShape(16.dp)
) {

    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
                .background(Color(0xFF713DFF)),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "W",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = viewers,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        Text(
            text = "LIVE",
            color = Color(0xFFFF4F7B),
            fontWeight = FontWeight.Bold
        )
    }
}

}

@androidx.compose.runtime.Composable
fun LiveScreen(
modifier: Modifier = Modifier
) {

Column(
    modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF08080D))
        .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {

    Spacer(modifier = Modifier.height(30.dp))

    Text(
        text = "WAVE LIVE",
        color = Color.White,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold
    )

    Spacer(modifier = Modifier.height(35.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF32146B),
                        Color(0xFF101017)
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
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF713DFF)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Live",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Live streams will appear here",
                color = Color.White,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = { },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF713DFF)
        ),
        shape = RoundedCornerShape(15.dp)
    ) {

        Text(
            text = "Start Watching",
            fontWeight = FontWeight.Bold
        )
    }
}

}

@androidx.compose.runtime.Composable
fun CreateScreen(
modifier: Modifier = Modifier
) {

Column(
    modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF08080D))
        .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {

    Spacer(modifier = Modifier.height(45.dp))

    Text(
        text = "Create Live",
        color = Color.White,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(25.dp))

    Text(
        text = "ابدأ البث المباشر على Wave",
        color = Color.LightGray,
        fontSize = 17.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(35.dp))

    Button(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF713DFF)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null
        )

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = "Start Live",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

}

@androidx.compose.runtime.Composable
fun InboxScreen(
modifier: Modifier = Modifier
) {

Column(
    modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF08080D))
        .padding(20.dp)
) {

    Text(
        text = "Inbox",
        color = Color.White,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(30.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF15151D),
        shape = RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Mail,
                contentDescription = null,
                tint = Color(0xFFB98CFF),
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Notifications",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "الإشعارات والرسائل ستظهر هنا",
                color = Color.Gray,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

}

@androidx.compose.runtime.Composable
fun ProfileScreen(
modifier: Modifier = Modifier
) {

Column(
    modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF08080D))
        .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {

    Spacer(modifier = Modifier.height(35.dp))

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xFF713DFF)),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = Color.White,
            modifier = Modifier.size(55.dp)
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = "Wave User",
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "Wave Live",
        color = Color(0xFFB98CFF),
        fontSize = 15.sp
    )

    Spacer(modifier = Modifier.height(30.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF15151D),
        shape = RoundedCornerShape(18.dp)
    ) {

        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
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
                label = "Coins"
            )
        }
    }

    Spacer(modifier = Modifier.height(25.dp))

    Button(
        onClick = { },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF713DFF)
        )
    ) {

        Text(
            text = "Edit Profile"
        )
    }
}

}

@androidx.compose.runtime.Composable
fun ProfileStat(
value: String,
label: String
) {

Column(
    horizontalAlignment = Alignment.CenterHorizontally
) {

    Text(
        text = value,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = label,
        color = Color.Gray,
        fontSize = 12.sp
    )
}

}
