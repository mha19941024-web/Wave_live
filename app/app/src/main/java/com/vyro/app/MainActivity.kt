package com.vyro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaveApp()
        }
    }
}

@Composable
fun WaveApp() {
    var selectedItem by remember { mutableIntStateOf(0) }

    val items = listOf(
        "الرئيسية",
        "بحث",
        "بث مباشر",
        "الإشعارات",
        "حسابي"
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedItem == 0,
                    onClick = { selectedItem = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "الرئيسية"
                        )
                    },
                    label = { Text("الرئيسية") }
                )

                NavigationBarItem(
                    selected = selectedItem == 1,
                    onClick = { selectedItem = 1 },
                    icon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "بحث"
                        )
                    },
                    label = { Text("بحث") }
                )

                NavigationBarItem(
                    selected = selectedItem == 2,
                    onClick = { selectedItem = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "بث مباشر"
                        )
                    },
                    label = { Text("بث") }
                )

                NavigationBarItem(
                    selected = selectedItem == 3,
                    onClick = { selectedItem = 3 },
                    icon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "الإشعارات"
                        )
                    },
                    label = { Text("نشاط") }
                )

                NavigationBarItem(
                    selected = selectedItem == 4,
                    onClick = { selectedItem = 4 },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "حسابي"
                        )
                    },
                    label = { Text("حسابي") }
                )
            }
        }
    ) { paddingValues ->

        when (selectedItem) {
            0 -> HomeScreen(
                modifier = Modifier.padding(paddingValues)
            )

            1 -> SearchScreen(
                modifier = Modifier.padding(paddingValues)
            )

            2 -> LiveScreen(
                modifier = Modifier.padding(paddingValues)
            )

            3 -> ActivityScreen(
                modifier = Modifier.padding(paddingValues)
            )

            4 -> ProfileScreen(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .padding(16.dp)
    ) {

        item {
            Text(
                text = "Wave",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "اكتشف أفضل الفيديوهات والبثوث المباشرة",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = "البث المباشر الآن",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        items(
            listOf(
                "Wave Live",
                "Live Creator",
                "Arabic Live",
                "Music Live"
            )
        ) { creator ->

            LiveCard(creator)
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "فيديوهات مقترحة",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        items(
            listOf(
                "فيديو جديد من Wave",
                "أفضل لحظات البث",
                "محتوى جديد اليوم"
            )
        ) { title ->

            VideoCard(title)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun LiveCard(
    creator: String,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF673AB7)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "W",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = creator,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "🔴 مباشر الآن",
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "مشاهدة"
            )
        }
    }
}

@Composable
fun VideoCard(
    title: String,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp
    ) {

        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(Color(0xFF303030)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(55.dp)
                )
            }

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Wave",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                Icons.Default.Search,
                contentDescription = "بحث",
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "البحث في Wave",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LiveScreen(modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Wave Live",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    // سيتم ربط بدء البث بالـ backend لاحقاً
                }
            ) {

                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text("ابدأ البث المباشر")
            }
        }
    }
}

@Composable
fun ActivityScreen(modifier: Modifier = Modifier) {

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "الإشعارات والنشاط",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF673AB7)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "W",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "حساب Wave",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "مرحباً بك في Wave"
            )
        }
    }
}

مهم: استبدل محتوى ملف "MainActivity.kt" بالكامل بهذا النص، ولا تضف أي جزء منه إلى الملف القديم حتى لا تتكرر الـ imports أو الدوال.

ولو ظهر خطأ بعد الرفع، ابعت لي نص الخطأ فقط وسأعدل الملف بناءً عليه.
