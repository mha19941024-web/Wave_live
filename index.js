export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method === 'OPTIONS') return new Response(null,{headers:cors()});
    if (url.pathname === '/health') return json({ok:true,service:'Wave Server',version:'1.0.0'});
    if (url.pathname === '/gifts') return json({currency:'Wave Coins',gifts:[{id:'crown',name:'التاج الدوّار',price:500,exclusive:true},{id:'lion',name:'الأسد',price:250},{id:'tiger',name:'النمر',price:150},{id:'diamond',name:'الماسة',price:100},{id:'star',name:'النجمة',price:50}]});
    return json({ok:true,message:'Wave Server is working!'});
  }
};
function cors(){return {'Access-Control-Allow-Origin':'*','Access-Control-Allow-Methods':'GET,POST,OPTIONS','Access-Control-Allow-Headers':'Content-Type,Authorization'}}
function json(x){return new Response(JSON.stringify(x),{headers:{...cors(),'content-type':'application/json; charset=UTF-8'}})}
