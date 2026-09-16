package com.vyro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
MaterialTheme {
Surface(
modifier = Modifier.fillMaxSize(),
color = MaterialTheme.colorScheme.background
) {
Box(
modifier = Modifier
.fillMaxSize()
.padding(24.dp),
contentAlignment = Alignment.Center
) {
Column(
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.spacedBy(16.dp)
) {
Text(
text = "WAVE",
style = MaterialTheme.typography.headlineLarge
)

                Text(
                    text = "Wave Live"
                )

                Button(
                    onClick = {
                        // سيتم ربط وظائف البث المباشر هنا لاحقًا
                    }
                ) {
                    Text("ابدأ البث")
                }
            }
        }
    }
}

}
