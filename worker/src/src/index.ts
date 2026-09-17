export interface Env {
  DB: D1Database;
  ENVIRONMENT?: string;
  CLOUDFLARE_ACCOUNT_ID?: string;
  CLOUDFLARE_API_TOKEN?: string;
  STREAM_CUSTOMER_SUBDOMAIN?: string;
  SESSION_DAYS?: string;
  CORS_ORIGIN?: string;
}

type VideoRow = { id:string; url:string; user:string; caption:string; likes:number; created_at:string };
const bearer=(r:Request)=>{const h=r.headers.get("Authorization")??"";return h.startsWith("Bearer ")?h.slice(7):null};
const username=()=>`user_${crypto.randomUUID().replaceAll("-","").slice(0,10)}`;
const json=(data:unknown,status=200)=>Response.json(data,{status,headers:{"Cache-Control":"no-store"}});
function cors(r:Response,env:Env){r.headers.set("Access-Control-Allow-Origin",env.CORS_ORIGIN??"*");r.headers.set("Access-Control-Allow-Headers","Content-Type, Authorization");r.headers.set("Access-Control-Allow-Methods","GET,POST,PATCH,DELETE,OPTIONS");return r}
const out=(data:unknown,status=200,env?:Env)=>env?cors(json(data,status),env):json(data,status);
async function auth(request:Request,env:Env){const token=bearer(request);if(!token)return null;const row=await env.DB.prepare("SELECT user_id FROM sessions WHERE token=? AND expires_at>? ").bind(token,new Date().toISOString()).first<{user_id:string}>();return row?.user_id??null}
function cfHeaders(env:Env){return {Authorization:`Bearer ${env.CLOUDFLARE_API_TOKEN}`,"Content-Type":"application/json"}}

export default {async fetch(request:Request,env:Env):Promise<Response>{
  if(request.method==="OPTIONS")return cors(new Response(null,{status:204}),env);
  const url=new URL(request.url);
  try{
    if(url.pathname==="/health")return out({ok:true,service:"vyro-api",version:"0.9.0",environment:env.ENVIRONMENT??"unknown"},200,env);
    if(url.pathname==="/api/session"&&request.method==="POST"){
      const userId=crypto.randomUUID(),token=crypto.randomUUID()+crypto.randomUUID(),now=new Date();
      const days=Math.min(Math.max(Number(env.SESSION_DAYS??30),1),90),expires=new Date(now.getTime()+days*86400000).toISOString(),user=username();
      await env.DB.batch([
        env.DB.prepare("INSERT INTO users(id,username,display_name,bio,avatar_url,created_at) VALUES(?,?,?,?,?,?)").bind(userId,user,"","","",now.toISOString()),
        env.DB.prepare("INSERT INTO sessions(token,user_id,created_at,expires_at) VALUES(?,?,?,?)").bind(token,userId,now.toISOString(),expires),
        env.DB.prepare("INSERT INTO wallets(user_id,coins,updated_at) VALUES(?,?,?)").bind(userId,0,now.toISOString())
      ]);return out({token,user:{id:userId,username:user,displayName:"",bio:"",avatarUrl:""},wallet:{coins:0},expiresAt:expires},201,env)
    }
    const userId=await auth(request,env);
    if(url.pathname==="/api/me"&&request.method==="GET"){
      if(!userId)return out({error:"unauthorized"},401,env);
      const user=await env.DB.prepare("SELECT id,username,display_name AS displayName,bio,avatar_url AS avatarUrl,created_at FROM users WHERE id=?").bind(userId).first();return user?out({user},200,env):out({error:"user not found"},404,env)
    }
    if(url.pathname==="/api/feed"&&request.method==="GET"){
      const limit=Math.min(Math.max(Number(url.searchParams.get("limit")??20),1),50),cursor=Math.max(Number(url.searchParams.get("cursor")??0),0);
      const rows=await env.DB.prepare("SELECT v.id,v.url,v.user,v.caption,v.likes,v.created_at FROM videos v LEFT JOIN users u ON u.username=v.user WHERE u.id IS NULL OR NOT EXISTS (SELECT 1 FROM blocks b WHERE b.blocker_id=? AND b.blocked_id=u.id) ORDER BY v.created_at DESC LIMIT ? OFFSET ?").bind(userId??"",limit,cursor).all<VideoRow>();return out({items:rows.results,nextCursor:rows.results.length===limit?cursor+rows.results.length:null},200,env)
    }
    if(url.pathname==="/api/videos"&&request.method==="POST"){
      if(!userId)return out({error:"unauthorized"},401,env);const body=await request.json() as {url?:unknown;streamId?:unknown;caption?:unknown};
      let playback=typeof body.url==="string"?body.url.trim():"";
      if(!playback&&typeof body.streamId==="string"&&env.STREAM_CUSTOMER_SUBDOMAIN)playback=`https://${env.STREAM_CUSTOMER_SUBDOMAIN}.cloudflarestream.com/${encodeURIComponent(body.streamId)}/manifest/video.m3u8`;
      if(!playback)return out({error:"url or streamId is required"},400,env);
      const user=await env.DB.prepare("SELECT username AS user FROM users WHERE id=?").bind(userId).first<{user:string}>(),id=crypto.randomUUID(),now=new Date().toISOString(),caption=typeof body.caption==="string"?body.caption.trim().slice(0,500):"";
      await env.DB.prepare("INSERT INTO videos(id,url,user,caption,likes,created_at) VALUES(?,?,?,?,0,?)").bind(id,playback,user?.user??"user",caption,now).run();return out({id,url:playback,user:user?.user??"user",caption,likes:0,created_at:now},201,env)
    }
    const like=url.pathname.match(/^\/api\/videos\/([^/]+)\/like$/);
    if(like&&request.method==="POST"){
      if(!userId)return out({error:"unauthorized"},401,env);if(!(await env.DB.prepare("SELECT id FROM videos WHERE id=?").bind(like[1]).first()))return out({error:"video not found"},404,env);
      const r=await env.DB.prepare("INSERT OR IGNORE INTO video_likes(video_id,user_id,created_at) VALUES(?,?,?)").bind(like[1],userId,new Date().toISOString()).run();if(r.meta.changes)await env.DB.prepare("UPDATE videos SET likes=likes+1 WHERE id=?").bind(like[1]).run();const row=await env.DB.prepare("SELECT likes FROM videos WHERE id=?").bind(like[1]).first<{likes:number}>();return out({id:like[1],likes:row?.likes??0,liked:true},200,env)
    }
    if(like&&request.method==="DELETE"){
      if(!userId)return out({error:"unauthorized"},401,env);const r=await env.DB.prepare("DELETE FROM video_likes WHERE video_id=? AND user_id=?").bind(like[1],userId).run();if(r.meta.changes)await env.DB.prepare("UPDATE videos SET likes=MAX(likes-1,0) WHERE id=?").bind(like[1]).run();const row=await env.DB.prepare("SELECT likes FROM videos WHERE id=?").bind(like[1]).first<{likes:number}>();return out({id:like[1],likes:row?.likes??0,liked:false},200,env)
    }
    const follow=url.pathname.match(/^\/api\/users\/([^/]+)\/follow$/);
    if(follow&&(request.method==="POST"||request.method==="DELETE")){
      if(!userId)return out({error:"unauthorized"},401,env);const target=await env.DB.prepare("SELECT id FROM users WHERE id=? OR username=?").bind(follow[1],follow[1]).first<{id:string}>();if(!target)return out({error:"user not found"},404,env);if(target.id===userId)return out({error:"cannot follow yourself"},400,env);
      if(request.method==="POST")await env.DB.prepare("INSERT OR IGNORE INTO follows(follower_id,following_id,created_at) VALUES(?,?,?)").bind(userId,target.id,new Date().toISOString()).run();else await env.DB.prepare("DELETE FROM follows WHERE follower_id=? AND following_id=?").bind(userId,target.id).run();return out({following:request.method==="POST",userId:target.id},200,env)
    }
    if(url.pathname==="/api/profile"&&request.method==="PATCH"){
      if(!userId)return out({error:"unauthorized"},401,env);const body=await request.json() as {displayName?:unknown;bio?:unknown;avatarUrl?:unknown};
      const d=typeof body.displayName==="string"?body.displayName.trim().slice(0,80):null,b=typeof body.bio==="string"?body.bio.trim().slice(0,500):null,a=typeof body.avatarUrl==="string"?body.avatarUrl.trim().slice(0,500):null;
      await env.DB.prepare("UPDATE users SET display_name=COALESCE(?,display_name),bio=COALESCE(?,bio),avatar_url=COALESCE(?,avatar_url) WHERE id=?").bind(d,b,a,userId).run();const user=await env.DB.prepare("SELECT id,username,display_name AS displayName,bio,avatar_url AS avatarUrl FROM users WHERE id=?").bind(userId).first();return out({user},200,env)
    }
    const comments=url.pathname.match(/^\/api\/videos\/([^/]+)\/comments$/);
    if(comments&&request.method==="GET"){const limit=Math.min(Math.max(Number(url.searchParams.get("limit")??30),1),100);const rows=await env.DB.prepare("SELECT c.id,c.text,c.created_at,u.username AS user,u.avatar_url AS avatarUrl FROM comments c JOIN users u ON u.id=c.user_id WHERE c.video_id=? ORDER BY c.created_at DESC LIMIT ?").bind(comments[1],limit).all();return out({items:rows.results},200,env)}
    if(comments&&request.method==="POST"){
      if(!userId)return out({error:"unauthorized"},401,env);const body=await request.json() as {text?:unknown};if(typeof body.text!=="string"||!body.text.trim()||body.text.length>500)return out({error:"comment must be 1-500 chars"},400,env);if(!(await env.DB.prepare("SELECT id FROM videos WHERE id=?").bind(comments[1]).first()))return out({error:"video not found"},404,env);const id=crypto.randomUUID(),now=new Date().toISOString();await env.DB.prepare("INSERT INTO comments(id,video_id,user_id,text,created_at) VALUES(?,?,?,?,?)").bind(id,comments[1],userId,body.text.trim(),now).run();const u=await env.DB.prepare("SELECT username AS user,avatar_url AS avatarUrl FROM users WHERE id=?").bind(userId).first();return out({id,text:body.text.trim(),created_at:now,...u},201,env)
    }
    if(url.pathname==="/api/account"&&request.method==="DELETE"){
      if(!userId)return out({error:"unauthorized"},401,env);
      await env.DB.batch([
        env.DB.prepare("DELETE FROM comments WHERE user_id=?").bind(userId),
        env.DB.prepare("DELETE FROM video_likes WHERE user_id=?").bind(userId),
        env.DB.prepare("DELETE FROM follows WHERE follower_id=? OR following_id=?").bind(userId,userId),
        env.DB.prepare("DELETE FROM blocks WHERE blocker_id=? OR blocked_id=?").bind(userId,userId),
        env.DB.prepare("DELETE FROM reports WHERE reporter_id=? OR reported_user_id=?").bind(userId,userId),
        env.DB.prepare("DELETE FROM sessions WHERE user_id=?").bind(userId),
        env.DB.prepare("DELETE FROM users WHERE id=?").bind(userId)
      ]);
      return out({ok:true},200,env);
    }
    const block=url.pathname.match(/^\/api\/users\/([^/]+)\/block$/);
    if(block&&(request.method==="POST"||request.method==="DELETE")){
      if(!userId)return out({error:"unauthorized"},401,env);
      const target=await env.DB.prepare("SELECT id FROM users WHERE id=? OR username=?").bind(block[1],block[1]).first<{id:string}>();
      if(!target)return out({error:"user not found"},404,env);
      if(target.id===userId)return out({error:"cannot block yourself"},400,env);
      if(request.method==="POST") await env.DB.prepare("INSERT OR IGNORE INTO blocks(blocker_id,blocked_id,created_at) VALUES(?,?,?)").bind(userId,target.id,new Date().toISOString()).run();
      else await env.DB.prepare("DELETE FROM blocks WHERE blocker_id=? AND blocked_id=?").bind(userId,target.id).run();
      return out({blocked:request.method==="POST",userId:target.id},200,env);
    }
    if(url.pathname==="/api/reports"&&request.method==="POST"){
      if(!userId)return out({error:"unauthorized"},401,env);
      const body=await request.json() as {videoId?:unknown;reportedUserId?:unknown;reason?:unknown;details?:unknown};
      if(typeof body.reason!=="string"||!body.reason.trim())return out({error:"reason is required"},400,env);
      const id=crypto.randomUUID(),now=new Date().toISOString();
      await env.DB.prepare("INSERT INTO reports(id,reporter_id,video_id,reported_user_id,reason,details,created_at) VALUES(?,?,?,?,?,?,?)").bind(id,userId,typeof body.videoId==="string"?body.videoId:null,typeof body.reportedUserId==="string"?body.reportedUserId:null,body.reason.trim().slice(0,100),typeof body.details==="string"?body.details.trim().slice(0,1000):"",now).run();
      return out({ok:true,id},201,env);
    }
    if(url.pathname==="/api/upload/direct"&&request.method==="POST"){
      if(!userId)return out({error:"unauthorized"},401,env);if(!env.CLOUDFLARE_ACCOUNT_ID||!env.CLOUDFLARE_API_TOKEN)return out({error:"Upload service is not configured",code:"CLOUDFLARE_NOT_CONFIGURED"},503,env);
      const cf=await fetch(`https://api.cloudflare.com/client/v4/accounts/${env.CLOUDFLARE_ACCOUNT_ID}/stream/direct_upload`,{method:"POST",headers:cfHeaders(env),body:JSON.stringify({maxDurationSeconds:600})});const payload=await cf.json();return out(cf.ok?payload:{error:"Cloudflare upload creation failed",details:payload},cf.ok?201:502,env)
    }
    if(url.pathname==="/api/gifts"&&request.method==="GET"){
      const rows=await env.DB.prepare("SELECT id,name,icon,price_coins AS priceCoins FROM gift_catalog WHERE active=1 ORDER BY sort_order ASC").all();
      return out({items:rows.results},200,env);
    }
    const live=url.pathname.match(/^\/api\/live\/([^/]+)$/);
    if(live&&request.method==="GET"){
      const row=await env.DB.prepare("SELECT l.id,l.title,l.status,l.created_at AS createdAt,u.username AS host FROM live_sessions l JOIN users u ON u.id=l.host_user_id WHERE l.id=?").bind(live[1]).first();
      return row?out({live:row},200,env):out({error:"live not found"},404,env);
    }
    if(live&&request.method==="PATCH"){
      if(!userId)return out({error:"unauthorized"},401,env);const body=await request.json() as {status?:unknown;title?:unknown};
      const row=await env.DB.prepare("SELECT host_user_id FROM live_sessions WHERE id=?").bind(live[1]).first<{host_user_id:string}>();if(!row)return out({error:"live not found"},404,env);if(row.host_user_id!==userId)return out({error:"forbidden"},403,env);
      const status=typeof body.status==="string"&&["created","live","ended"].includes(body.status)?body.status:null;const title=typeof body.title==="string"?body.title.trim().slice(0,120):null;
      await env.DB.prepare("UPDATE live_sessions SET status=COALESCE(?,status),title=COALESCE(?,title),ended_at=CASE WHEN ?='ended' THEN ? ELSE ended_at END WHERE id=?").bind(status,title,status,new Date().toISOString(),live[1]).run();return out({ok:true},200,env);
    }
    const sendGift=url.pathname.match(/^\/api\/live\/([^/]+)\/gifts$/);
    if(sendGift&&request.method==="POST"){
      if(!userId)return out({error:"unauthorized"},401,env);const body=await request.json() as {giftId?:unknown;quantity?:unknown;receiverUserId?:unknown};
      const giftId=typeof body.giftId==="string"?body.giftId:"",quantity=Math.min(Math.max(Number(body.quantity??1),1),100);if(!giftId||!Number.isInteger(quantity))return out({error:"giftId and valid quantity are required"},400,env);
      const liveRow=await env.DB.prepare("SELECT host_user_id,status FROM live_sessions WHERE id=?").bind(sendGift[1]).first<{host_user_id:string;status:string}>();if(!liveRow)return out({error:"live not found"},404,env);if(liveRow.status!=="live")return out({error:"live is not active"},409,env);
      const gift=await env.DB.prepare("SELECT id,name,price_coins AS priceCoins FROM gift_catalog WHERE id=? AND active=1").bind(giftId).first<{id:string;name:string;priceCoins:number}>();if(!gift)return out({error:"gift not found"},404,env);
      const receiver=typeof body.receiverUserId==="string"?body.receiverUserId:liveRow.host_user_id;if(receiver!==liveRow.host_user_id)return out({error:"receiver must be the live host"},400,env);
      const total=gift.priceCoins*quantity,now=new Date().toISOString(),tx=crypto.randomUUID();
      const wallet=await env.DB.prepare("SELECT coins FROM wallets WHERE user_id=?").bind(userId).first<{coins:number}>();if((wallet?.coins??0)<total)return out({error:"insufficient coins",requiredCoins:total,availableCoins:wallet?.coins??0},402,env);
      await env.DB.batch([env.DB.prepare("UPDATE wallets SET coins=coins-?,updated_at=? WHERE user_id=? AND coins>=?").bind(total,now,userId,total),env.DB.prepare("INSERT INTO gift_transactions(id,live_id,sender_user_id,receiver_user_id,gift_id,quantity,coins_total,created_at) VALUES(?,?,?,?,?,?,?,?)").bind(tx,sendGift[1],userId,receiver,giftId,quantity,total,now)]);
      return out({ok:true,transactionId:tx,gift:{id:gift.id,name:gift.name,priceCoins:gift.priceCoins,quantity},coinsSpent:total},201,env);
    }
    if(url.pathname==="/api/live/create"&&request.method==="POST"){
      if(!userId)return out({error:"unauthorized"},401,env);if(!env.CLOUDFLARE_ACCOUNT_ID||!env.CLOUDFLARE_API_TOKEN)return out({error:"Live service is not configured",code:"CLOUDFLARE_NOT_CONFIGURED"},503,env);
      const body=await request.json().catch(()=>({})) as {title?:unknown};
      const cf=await fetch(`https://api.cloudflare.com/client/v4/accounts/${env.CLOUDFLARE_ACCOUNT_ID}/stream/live_inputs`,{method:"POST",headers:cfHeaders(env),body:JSON.stringify({recording:{mode:"automatic"}})});const payload=await cf.json();if(!cf.ok)return out({error:"Cloudflare live input creation failed",details:payload},502,env);
      const result=(payload as any).result;const liveId=crypto.randomUUID();const title=typeof body.title==="string"?body.title.trim().slice(0,120):"";
      await env.DB.prepare("INSERT INTO live_sessions(id,host_user_id,title,status,cloudflare_input_id,created_at) VALUES(?,?,?,?,?,?)").bind(liveId,userId,title,"created",result.uid,new Date().toISOString()).run();return out({...payload,liveId},201,env)
    }
    return out({error:"not found"},404,env)
  }catch(e){console.error(e);return out({error:"internal server error"},500,env)}
}};
