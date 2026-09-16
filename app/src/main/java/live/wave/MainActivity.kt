package com.vyro.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val PREFS="vyro_session"
private const val API=BuildConfig.API_BASE_URL

data class Video(val id:String,val url:String,val user:String,val caption:String,val likes:Int)
data class Me(val id:String,val username:String,val displayName:String,val bio:String)
enum class Tab{HOME,LIVE,CREATE,INBOX,PROFILE}

class MainActivity:ComponentActivity(){
    override fun onCreate(s:Bundle?){
        super.onCreate(s)
        setContent{WaveRoot()}
    }
}

@Composable
fun WaveRoot(){
    var started by rememberSaveable { mutableStateOf(false) }
    if(!started) SplashScreen(onStart={ started=true }) else WaveApp(initialTab=Tab.LIVE)
}

@Composable
fun SplashScreen(onStart:()->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            Text("WAVE",color=Color.White,style=MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text("Wave Live",color=Color.White,style=MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(52.dp))
            Button(onClick=onStart,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF6F4BB8)),modifier=Modifier.width(200.dp).height(56.dp)){
                Text("Start Live",style=MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable fun WaveApp(initialTab:Tab=Tab.HOME){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var token by remember{mutableStateOf(context.getSharedPreferences(PREFS,0).getString("token",null))};var tab by rememberSaveable{mutableStateOf(initialTab)}
 var videos by remember{mutableStateOf<List<Video>>(emptyList())};var me by remember{mutableStateOf<Me?>(null)};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)}
 suspend fun ensureSession(){if(token==null){token=Api.session();context.getSharedPreferences(PREFS,0).edit().putString("token",token).apply()}}
 fun load(){scope.launch{busy=true;message=null;try{ensureSession();videos=Api.feed(token!!);me=Api.me(token!!)}catch(e:Exception){message=e.message?:("Connection error")}finally{busy=false}}}
 LaunchedEffect(Unit){load()}
 Scaffold(containerColor=Color.Black,bottomBar={NavigationBar(containerColor=Color.Black){Nav(Tab.HOME,Icons.Default.Home,"Home",tab){tab=it};Nav(Tab.LIVE,Icons.Default.LiveTv,"LIVE",tab){tab=it};Nav(Tab.CREATE,Icons.Default.AddCircle,"Create",tab){tab=it};Nav(Tab.INBOX,Icons.Default.Notifications,"Inbox",tab){tab=it};Nav(Tab.PROFILE,Icons.Default.Person,"Profile",tab){tab=it}}}){p->Box(Modifier.fillMaxSize().padding(p)){when(tab){Tab.HOME->Home(videos,busy,message,{load()},{v,liked->scope.launch{try{ensureSession();Api.like(token!!,v.id,liked);videos=Api.feed(token!!)}catch(e:Exception){message=e.message}}}};Tab.LIVE->Live(token,{busy=it},{message=it});Tab.CREATE->Create(token,{busy=it},{msg->message=msg;load()});Tab.INBOX->Center("INBOX\nالإشعارات قريباً");Tab.PROFILE->Profile(me,{name,bio->scope.launch{try{ensureSession();me=Api.profile(token!!,name,bio);message="تم حفظ الملف"}catch(e:Exception){message=e.message}}})};if(message!=null)Text(message!!,color=Color.White,modifier=Modifier.align(Alignment.TopCenter).padding(8.dp))}}}
}

@Composable fun Nav(t:Tab,icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,sel:Tab,on:(Tab)->Unit)=NavigationBarItem(t==sel,{on(t)},{Icon(icon,label)},label={Text(label)})

@Composable fun Home(videos:List<Video>,busy:Boolean,error:String?,retry:()->Unit,onLike:(Video,Boolean)->Unit){Column(Modifier.fillMaxSize().background(Color.Black)){if(error!=null)Text("$error  ",color=Color.White,modifier=Modifier.padding(12.dp));if(busy&&videos.isEmpty())Center("VYRO\nLoading…") else if(videos.isEmpty())Center("لا توجد فيديوهات بعد") else LazyColumn{items(videos,key={it.id}){v->VideoCard(v,onLike)}}}}

@Composable fun VideoCard(v:Video,onLike:(Video,Boolean)->Unit){var liked by remember(v.id){mutableStateOf(false)};Column(Modifier.fillMaxWidth().padding(bottom=12.dp)){Box(Modifier.fillMaxWidth().height(620.dp)){Player(v.url);Column(Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(.78f)){Text("@${v.user}",color=Color.White,style=MaterialTheme.typography.titleMedium);Text(v.caption,color=Color.White)}}Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){IconButton({liked=!liked;onLike(v,liked)}){Icon(if(liked)Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Like",tint=Color.White)}Text(v.likes.toString(),color=Color.White);Spacer(Modifier.width(12.dp));Icon(Icons.Default.ChatBubble,"Comments",tint=Color.White);Spacer(Modifier.weight(1f));Icon(Icons.Default.Share,"Share",tint=Color.White)}}}

@Composable fun Player(url:String){val c=LocalContext.current;val p=remember(url){ExoPlayer.Builder(c).build().apply{setMediaItem(MediaItem.fromUri(url));prepare();playWhenReady=false}};DisposableEffect(Unit){onDispose{p.release()}};AndroidView({PlayerView(it).apply{useController=true;player=p}},Modifier.fillMaxSize())}

@Composable fun Create(token:String?,setBusy:(Boolean)->Unit,done:(String)->Unit){val context=LocalContext.current;val scope=rememberCoroutineScope();var caption by remember{mutableStateOf("")};var selected by remember{mutableStateOf<Uri?>(null)};var status by remember{mutableStateOf("")};val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){selected=it};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.Center){Text("CREATE",color=Color.White,style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(16.dp));OutlinedTextField(caption,{caption=it},label={Text("وصف الفيديو")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));Button({picker.launch("video/*")},modifier=Modifier.fillMaxWidth()){Text(if(selected==null)"اختيار فيديو من الهاتف" else "تم اختيار الفيديو")};Spacer(Modifier.height(12.dp));Button(enabled=selected!=null&&!token.isNullOrBlank(),onClick={scope.launch{setBusy(true);status="جاري الرفع…";try{val ticket=Api.directUpload(token!!);val id=Api.uploadFile(context,selected!!,ticket.uploadUrl);Api.createVideo(token,id,caption);status="تم النشر بنجاح";done(status)}catch(e:Exception){status=e.message?:"فشل الرفع";done(status)}finally{setBusy(false)}}},modifier=Modifier.fillMaxWidth()){Text("رفع ونشر")};Text(status,color=Color.White,modifier=Modifier.padding(top=12.dp))}}

@Composable fun Live(token:String?,setBusy:(Boolean)->Unit,onMessage:(String)->Unit){val scope=rememberCoroutineScope();var rtmp by remember{mutableStateOf("")};var key by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.Center){Text("LIVE",color=Color.White,style=MaterialTheme.typography.headlineMedium);Text("أنشئ قناة بث من Cloudflare Stream ثم استخدم RTMPS URL وStream Key في برنامج البث.",color=Color.LightGray);Spacer(Modifier.height(18.dp));Button(enabled=!token.isNullOrBlank(),onClick={scope.launch{setBusy(true);try{val x=Api.live(token!!);rtmp=x.rtmps;key=x.key;onMessage("تم إنشاء قناة البث")}catch(e:Exception){onMessage(e.message?:"فشل إنشاء البث")}finally{setBusy(false)}}}){Text("إنشاء بث مباشر")};if(rtmp.isNotEmpty()){Spacer(Modifier.height(18.dp));Text("RTMPS URL",color=Color.Gray);Text(rtmp,color=Color.White);Spacer(Modifier.height(8.dp));Text("STREAM KEY",color=Color.Gray);Text(key,color=Color.White)}}}

@Composable fun Profile(me:Me?,save:(String,String)->Unit){var name by remember(me?.displayName){mutableStateOf(me?.displayName.orEmpty())};var bio by remember(me?.bio){mutableStateOf(me?.bio.orEmpty())};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.Center){Text("PROFILE",color=Color.White,style=MaterialTheme.typography.headlineMedium);Text("@${me?.username?:"guest"}",color=Color.LightGray);Spacer(Modifier.height(12.dp));OutlinedTextField(name,{name=it},label={Text("الاسم")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp));OutlinedTextField(bio,{bio=it},label={Text("نبذة")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));Button({save(name,bio)},modifier=Modifier.fillMaxWidth()){Text("حفظ")}}}
@Composable fun Center(text:String){Box(Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){Text(text,color=Color.White,style=MaterialTheme.typography.headlineSmall)}}

data class UploadTicket(val uploadUrl:String,val id:String)
data class LiveResult(val rtmps:String,val key:String)

object Api{
 private fun conn(path:String,method:String,token:String?=null):HttpURLConnection=(URL(API+path).openConnection() as HttpURLConnection).apply{requestMethod=method;connectTimeout=15000;readTimeout=30000;if(token!=null)setRequestProperty("Authorization","Bearer $token");if(method=="POST"||method=="PATCH"){doOutput=true;setRequestProperty("Content-Type","application/json")}}
 private fun body(c:HttpURLConnection):String{val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream;val text=stream?.bufferedReader()?.use{it.readText()}.orEmpty();if(code !in 200..299)throw IllegalStateException(JSONObject(text).optString("error","HTTP $code"));return text}
 suspend fun session()=withContext(Dispatchers.IO){val c=conn("/api/session","POST");try{JSONObject(body(c)).getString("token")}finally{c.disconnect()}}
 suspend fun feed(t:String)=withContext(Dispatchers.IO){val c=conn("/api/feed?limit=30","GET",t);try{val a=JSONObject(body(c)).optJSONArray("items")?:JSONArray();List(a.length()){val o=a.getJSONObject(it);Video(o.getString("id"),o.getString("url"),o.getString("user"),o.optString("caption"),o.optInt("likes"))}}finally{c.disconnect()}}
 suspend fun me(t:String)=withContext(Dispatchers.IO){val c=conn("/api/me","GET",t);try{val o=JSONObject(body(c)).getJSONObject("user");Me(o.getString("id"),o.getString("username"),o.optString("displayName"),o.optString("bio"))}finally{c.disconnect()}}
 suspend fun like(t:String,id:String,liked:Boolean)=withContext(Dispatchers.IO){val c=conn("/api/videos/$id/like",if(liked)"POST" else "DELETE",t);try{body(c)}finally{c.disconnect()}}
 suspend fun profile(t:String,name:String,bio:String)=withContext(Dispatchers.IO){val c=conn("/api/profile","PATCH",t);try{c.outputStream.use{it.write(JSONObject().put("displayName",name).put("bio",bio).toString().toByteArray())};val o=JSONObject(body(c)).getJSONObject("user");Me(o.getString("id"),o.getString("username"),o.optString("displayName"),o.optString("bio"))}finally{c.disconnect()}}
 suspend fun directUpload(t:String)=withContext(Dispatchers.IO){val c=conn("/api/upload/direct","POST",t);try{val r=JSONObject(body(c)).getJSONObject("result");UploadTicket(r.getString("uploadURL"),r.getString("uid"))}finally{c.disconnect()}}
 suspend fun createVideo(t:String,id:String,caption:String)=withContext(Dispatchers.IO){val c=conn("/api/videos","POST",t);try{c.outputStream.use{it.write(JSONObject().put("streamId",id).put("caption",caption).toString().toByteArray())};body(c)}finally{c.disconnect()}}
 suspend fun uploadFile(context:Context,uri:Uri,uploadUrl:String)=withContext(Dispatchers.IO){val boundary="----VYRO${System.currentTimeMillis()}";val c=(URL(uploadUrl).openConnection() as HttpURLConnection).apply{requestMethod="POST";doOutput=true;connectTimeout=30000;readTimeout=120000;setRequestProperty("Content-Type","multipart/form-data; boundary=$boundary")};try{DataOutputStream(c.outputStream).use{out->out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"video.mp4\"\r\nContent-Type: video/mp4\r\n\r\n");context.contentResolver.openInputStream(uri)?.use{input->val buf=ByteArray(64*1024);while(true){val n=input.read(buf);if(n<=0)break;out.write(buf,0,n)}}?:throw IllegalStateException("Cannot read selected file");out.writeBytes("\r\n--$boundary--\r\n")};if(c.responseCode !in 200..299)throw IllegalStateException("Upload failed: HTTP ${c.responseCode}");idFromUploadResponse(c.inputStream.bufferedReader().use{it.readText()})}finally{c.disconnect()}}
 private fun idFromUploadResponse(text:String)=JSONObject(text).optJSONObject("result")?.optString("uid")?:throw IllegalStateException("Cloudflare did not return a video id")
 suspend fun live(t:String)=withContext(Dispatchers.IO){val c=conn("/api/live/create","POST",t);try{val r=JSONObject(body(c)).getJSONObject("result");val rt=r.optJSONObject("rtmps")?:throw IllegalStateException("Cloudflare did not return RTMPS credentials");LiveResult(rt.optString("url"),rt.optString("streamKey"))}finally{c.disconnect()}}
}
