package com.vyro.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE =
    "https://worker-jolly-band-100e.mha19941024.workers.dev"

private const val PREFS = "wave_live_session"
private const val TOKEN_KEY = "token"

private val WaveBlack = Color(0xFF07060B)
private val WaveSurface = Color(0xFF14111B)
private val WavePurple = Color(0xFF9B5CFF)
private val WavePink = Color(0xFFFF4FA3)
private val WaveGold = Color(0xFFFFC857)
private val WaveGreen = Color(0xFF20D889)

data class WaveUser(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = ""
)

data class WaveVideo(
    val id: String,
    val url: String,
    val user: String,
    val caption: String,
    val likes: Int,
    val createdAt: String = "",
    val liked: Boolean = false
)

data class WaveGift(
    val id: String,
    val name: String,
    val icon: String,
    val priceCoins: Int
)

data class WaveComment(
    val id: String,
    val text: String,
    val user: String
)

data class ApiResult(
    val code: Int,
    val body: String
)

object WaveApi {

    private fun connection(
        method: String,
        path: String,
        token: String? = null
    ): HttpURLConnection {
        val connection =
            URL(API_BASE + path).openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )
        }

        return connection
    }

    suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null
    ): ApiResult = withContext(Dispatchers.IO) {

        val c = connection(method, path, token)

        if (body != null) {
            c.doOutput = true
            c.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )

            c.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val code = c.responseCode

        val stream =
            if (code in 200..299) {
                c.inputStream
            } else {
                c.errorStream ?: c.inputStream
            }

        val text =
            stream.bufferedReader().use {
                it.readText()
            }

        c.disconnect()

        ApiResult(code, text)
    }

    suspend fun createSession(): ApiResult =
        request(
            method = "POST",
            path = "/api/session"
        )

    suspend fun me(token: String): ApiResult =
        request(
            method = "GET",
            path = "/api
