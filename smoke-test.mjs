const base=(process.argv[2]||'').replace(/\/$/,'');
if(!base){console.error('Usage: node smoke-test.mjs https://your-worker.workers.dev');process.exit(2)}
for(const path of ['/health']){const r=await fetch(base+path);const t=await r.text();console.log(path,r.status,t);if(!r.ok)process.exit(1)}
