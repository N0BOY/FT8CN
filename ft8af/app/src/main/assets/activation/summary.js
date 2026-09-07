/* Shared by the website and Android's bundled, network-isolated preview. */
(() => {
  'use strict';
  const gridPoint = grid => {
    if (!/^[A-R]{2}[0-9]{2}(?:[A-X]{2}(?:[0-9]{2})?)?$/i.test(grid || '')) return null;
    const g = grid.toUpperCase();
    let lon = (g.charCodeAt(0) - 65) * 20 - 180 + Number(g[2]) * 2;
    let lat = (g.charCodeAt(1) - 65) * 10 - 90 + Number(g[3]);
    let w = 2, h = 1;
    if (g.length >= 6) { w /= 24; h /= 24; lon += (g.charCodeAt(4) - 65) * w; lat += (g.charCodeAt(5) - 65) * h; }
    if (g.length === 8) { w /= 10; h /= 10; lon += Number(g[6]) * w; lat += Number(g[7]) * h; }
    return [lon + w / 2, lat + h / 2];
  };
  const near = (lon, origin) => origin + ((lon - origin + 540) % 360) - 180;
  const distance = (a, b) => {
    const r = Math.PI / 180;
    const d = Math.sin((b[1] - a[1]) * r / 2) ** 2 + Math.cos(a[1] * r) * Math.cos(b[1] * r) * Math.sin((b[0] - a[0]) * r / 2) ** 2;
    return 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, d)));
  };
  const utcTime = v => {
    if (!v || !/^\d{1,6}$/.test(v)) return '—';
    const s = (v.length % 2 ? '0' + v : v).padEnd(6, '0');
    return +s.slice(0,2) < 24 && +s.slice(2,4) < 60 && +s.slice(4) < 60 ? s.slice(0,2) + ':' + s.slice(2,4) : '—';
  };
  const model = data => {
    const origin = data.contacts.map(c => gridPoint(c.myGrid)).find(Boolean) || null;
    const points = data.contacts.map(c => ({ ...c, point: gridPoint(c.grid) })).filter(c => c.point);
    return { origin, points, bands: new Set(data.contacts.map(c => c.band).filter(Boolean)).size,
      farthest: origin && points.length ? Math.round(Math.max(...points.map(c => distance(origin, c.point)))) : null };
  };
  globalThis.FT8Summary = { gridPoint, near, distance, utcTime, model };
  if (typeof document === 'undefined') return;
  const assetRoot = new URL('.', document.currentScript.src);
  const $ = id => document.getElementById(id);
  const native = !!globalThis.FT8Share;
  let data, land, brandIcon, publicUrl = '', busy = false;
  const canvas = $('card'), ctx = canvas.getContext('2d');
  const color = { ink: '#e7ecf3', muted: '#8a96b1', accent: '#ffaf5e', signal: '#5cd6e8' };
  const text = (s, x, y, size = 24, fill = color.ink, weight = 400, max = 980, mono = false) => {
    ctx.fillStyle = fill; ctx.font = `${Math.round(weight / 100) * 100} ${size}px ${mono ? '"Geist Mono", monospace' : 'Geist, sans-serif'}`;
    let value = String(s);
    while (ctx.measureText(value).width > max && value.length > 1) value = value.slice(0, -2) + '…';
    ctx.fillText(value, x, y);
  };
  const line = (x1,y1,x2,y2,c) => { ctx.strokeStyle=c;ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(x1,y1);ctx.lineTo(x2,y2);ctx.stroke(); };
  const dot = (x,y,r,c) => {ctx.fillStyle=c;ctx.beginPath();ctx.arc(x,y,r,0,Math.PI*2);ctx.fill();};
  function drawMap(m) {
    const x=40,y=320,w=1000,h=500;
    ctx.save(); ctx.beginPath(); ctx.rect(x,y,w,h); ctx.clip();
    ctx.fillStyle='#0e131e';ctx.fillRect(x,y,w,h);
    const originLon=m.origin?.[0] ?? 0;
    const points=[...(m.origin?[m.origin]:[]),...m.points.map(c=>c.point)].map(p=>[near(p[0],originLon),p[1]]);
    const xs=points.map(p=>p[0]),ys=points.map(p=>p[1]);
    const lo=points.length?Math.min(...xs):-180,hi=points.length?Math.max(...xs):180;
    const bottom=points.length?Math.min(...ys):-60,top=points.length?Math.max(...ys):85;
    const scale=Math.min(w/(Math.max(18,hi-lo)*1.3),h/(Math.max(18,top-bottom)*1.5));
    const px=lon=>x+w/2+(lon-(lo+hi)/2)*scale;
    const py=lat=>y+h/2-(lat-(bottom+top)/2)*scale;
    for(let lon=-540;lon<=540;lon+=15)line(px(lon),y,px(lon),y+h,'#1d2538');
    for(let lat=-90;lat<=90;lat+=15)line(x,py(lat),x+w,py(lat),'#1d2538');
    ctx.fillStyle='#232c43';ctx.strokeStyle='#3e4862';ctx.lineWidth=1;
    for(const feature of land.features){
      const geom=feature.geometry;
      const polygons=geom.type==='Polygon'?[geom.coordinates]:geom.type==='MultiPolygon'?geom.coordinates:[];
      for(const poly of polygons) for(const shift of [-360,0,360]){
        ctx.beginPath();poly[0].forEach((p,i)=>i?ctx.lineTo(px(p[0]+shift),py(p[1])):ctx.moveTo(px(p[0]+shift),py(p[1])));ctx.closePath();ctx.fill();ctx.stroke();
      }
    }
    if(m.origin){
      const ox=px(m.origin[0]),oy=py(m.origin[1]);
      for(const c of m.points){
        const cx=px(near(c.point[0],originLon)),cy=py(c.point[1]);
        ctx.strokeStyle='#5cd6e880';ctx.lineWidth=1.6;ctx.beginPath();ctx.moveTo(ox,oy);
        ctx.quadraticCurveTo((ox+cx)/2,(oy+cy)/2-Math.min(45,Math.abs(cx-ox)*.1),cx,cy);ctx.stroke();
      }
      dot(ox,oy,16,'#ffaf5e25');dot(ox,oy,8,color.accent);dot(ox,oy,3,'#0e131e');
    }
    for(const c of m.points)dot(px(near(c.point[0],originLon)),py(c.point[1]),4,color.signal);
    if(m.origin){const ox=px(m.origin[0]),oy=py(m.origin[1]);dot(ox,oy,7,color.accent);}
    ctx.fillStyle='#07090fe8';ctx.fillRect(x+16,y+h-48,w-32,34);
    text(`${m.points.length} mapped / ${data.contacts.length} contacts${m.origin?'   •   Amber: activation   /   Cyan: contacts':'   •   Operator location unavailable'}`,x+28,y+h-25,18,color.muted);
    text('Natural Earth',x+w-150,y+28,16,color.muted);
    ctx.restore();
  }
  function draw() {
    const m=model(data);
    const bg=ctx.createLinearGradient(0,0,1080,1350);bg.addColorStop(0,'#161c2b');bg.addColorStop(.4,'#07090f');bg.addColorStop(1,'#050709');ctx.fillStyle=bg;ctx.fillRect(0,0,1080,1350);
    ctx.fillStyle=color.accent;ctx.fillRect(40,40,38,5);
    text('PARKS ON THE AIR',92,51,20,color.accent,600,560,true);
    text('POTA / ACTIVATION',765,51,18,color.muted,500,280,true);
    text(data.call || 'POTA activation',40,136,76,color.ink,700,1000,true);
    text(data.parks.join('  +  '),43,181,28,color.accent,500,1000,true);
    const date=new Date(data.start).toLocaleDateString('en-US',{day:'2-digit',month:'long',year:'numeric',timeZone:'UTC'});
    text(`${date}  ·  ${Math.round((data.end-data.start)/60000)} min on air  ·  UTC`,43,218,20,color.muted);
    line(40,242,1040,242,'#293143');
    const stats=[[`${data.contacts.length}`,'CONTACTS'],[`${m.bands}`,'BANDS'],[m.farthest===null?'—':m.farthest.toLocaleString('en-US'),'FARTHEST / KM']];
    stats.forEach(([value,label],i)=>{const x=40+i*340;text(value,x,283,34,color.ink,600,300,true);text(label,x,307,15,color.muted,500,300,true);});
    drawMap(m);
    text('FROM A PARK. TO THE WORLD.',40,867,22,color.accent,600);
    const shown=data.contacts.slice(0,12);
    shown.forEach((c,i)=>{const col=i%3,row=Math.floor(i/3),x=40+col*340,y=915+row*53;text(c.call,x,y,24,color.ink,600,290,true);text([c.band,c.mode].filter(Boolean).join(' / '),x,y+20,15,color.muted,400,300,true);});
    if(!shown.length)text('A day in the field. No contacts logged.',40,932,25,color.muted);
    text(data.contacts.length>12?`+ ${data.contacts.length-12} more contacts · Scan for the complete log`:'Scan to explore the map and complete contact log',40,1138,20,color.muted);
    line(40,1162,1040,1162,'#293143');
    text('Powered by',160,1206,20,color.muted);
    if(brandIcon)ctx.drawImage(brandIcon,40,1184,96,96);
    text('FT',160,1254,44,color.ink,600,100,true);
    text('8',213,1254,44,color.accent,700,60,true);
    text('AF',240,1254,44,color.ink,600,100,true);
    text('Take your radio outside.',390,1227,25,color.ink,500,450);
    text('ft8af.app',390,1262,20,color.accent,500,350,true);
    if(publicUrl){
      const qr=qrcode(0,'M');qr.addData(publicUrl);qr.make();
      const n=qr.getModuleCount(),size=Math.floor(168/(n+8)),side=(n+8)*size,left=1040-side,top=1176;
      ctx.fillStyle='#ffffff';ctx.fillRect(left,top,side,side);ctx.fillStyle='#07090f';
      for(let row=0;row<n;row++)for(let col=0;col<n;col++)if(qr.isDark(row,col))ctx.fillRect(left+(col+4)*size,top+(row+4)*size,size,size);
    }else{text('QR added',862,1220,19,color.muted);text('when shared',862,1250,19,color.muted);}
    canvas.setAttribute('aria-label',`${data.call || 'POTA'} at ${data.parks.join(', ')}. ${data.contacts.length} contacts, ${m.bands} bands. ${m.points.length} contacts mapped.`);
  }
  function setBusy(value){busy=value; $('share').disabled=value;$('save').disabled=value;}
  function error(message){$('status').textContent=message;$('status').hidden=false;setBusy(false);}
  function log(){
    $('contacts').replaceChildren();
    for(const c of data.contacts){const tr=document.createElement('tr');const date=/^\d{8}$/.test(c.date)?`${c.date.slice(0,4)}-${c.date.slice(4,6)}-${c.date.slice(6)}`:'';
      for(const value of [c.call,[c.band,c.mode].filter(Boolean).join(' / ')||'—',c.grid||'—',`${date} ${utcTime(c.time)}`]){const td=document.createElement('td');td.textContent=value;tr.append(td);}$('contacts').append(tr);}
    $('log-count').textContent=`${data.contacts.length} contacts · ${data.parks.join(' + ')}`;
  }
  async function show(value,url=''){
    data=value;publicUrl=url;
    try{
      if(!land){const response=await fetch(new URL('world_land.json',assetRoot));if(!response.ok)throw Error();land=await response.json();}
      await Promise.all([document.fonts.load('600 24px Geist'),document.fonts.load('600 24px "Geist Mono"')]);
      if(!brandIcon)brandIcon=await new Promise((resolve,reject)=>{const icon=new Image();icon.onload=()=>resolve(icon);icon.onerror=reject;icon.src=new URL('icon.svg',assetRoot).href;});
      draw();if(!$('activation-data'))log();$('summary').hidden=false;$('status').hidden=true;
      if($('export-actions'))$('export-actions').hidden=false;
      setBusy(false);
    }
    catch{error('The share image could not be prepared. Reopen this summary to try again.');}
  }
  async function imageFile(){const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/png'));if(!blob)throw Error('Could not create image');return new File([blob],`ft8af-${data.parks[0]}.png`,{type:'image/png'});}
  async function save(){try{const file=await imageFile(),url=URL.createObjectURL(file),a=document.createElement('a');a.href=url;a.download=file.name;a.click();setTimeout(()=>URL.revokeObjectURL(url),30000);}catch{error('Could not save the image. Please try again.');}}
  $('share').onclick=async()=>{
    if(busy)return;setBusy(true);
    if(native){$('status').textContent='Preparing your share…';$('status').hidden=false;FT8Share.publish();return;}
    try{const file=await imageFile();if(navigator.canShare?.({files:[file]}))await navigator.share({files:[file],title:`${data.call} · POTA activation`,text:`From a park to the world. Powered by ft8af. ${publicUrl}`});else await save();}
    catch(e){if(e.name!=='AbortError')error('Sharing is unavailable. Use Save image instead.');}finally{setBusy(false);}
  };
  $('save').onclick=save;
  globalThis.showActivation=show;
  globalThis.shareFailed=()=>error('Could not share this activation. Check your connection and try again.');
  globalThis.sharePublished=url=>{publicUrl=url;try{draw();FT8Share.image(canvas.toDataURL('image/png'));}catch{shareFailed();}};
  globalThis.shareFinished=()=>{$('status').hidden=true;setBusy(false);};
  if(native){$('save').hidden=true;$('disclosure').textContent='Sharing publishes your callsign, parks, dates and contact log at ft8af.app. The page is public and may appear in search engines. Notes are not included.';FT8Share.ready();}
  else if($('activation-data')){
    const id=new URLSearchParams(location.search).get('id');
    show(JSON.parse($('activation-data').textContent),`https://ft8af.app/activation?id=${id}`);
  }
  else{
    const id=new URLSearchParams(location.search).get('id');
    if(!/^[a-f0-9]{32}$/.test(id||''))error('This share link is incomplete or invalid.');
    else fetch(`/api/activations?id=${id}`).then(async r=>{const value=await r.json();if(!r.ok)throw Error(value.error || 'Could not load activation');return show(value,`https://ft8af.app/activation?id=${id}`);}).catch(e=>error(e.message || 'Could not load activation. Please try again.'));
  }
})();
