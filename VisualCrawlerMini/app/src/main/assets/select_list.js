(function(){
  const STYLE_ID='vc_select_style_v09';
  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style');
    s.id=STYLE_ID;
    s.textContent='.__vc_list_pick__{outline:6px solid #ffd000!important;outline-offset:3px!important;background:rgba(255,208,0,.16)!important}.__vc_body_pick__{outline:6px solid #00e676!important;outline-offset:3px!important;background:rgba(0,230,118,.14)!important}.__vc_badge__{position:fixed!important;z-index:2147483647!important;left:12px!important;bottom:12px!important;padding:8px 12px!important;border-radius:999px!important;background:#111!important;color:white!important;font:700 14px sans-serif!important;box-shadow:0 4px 18px rgba(0,0,0,.35)!important}';
    document.documentElement.appendChild(s);
  }
  function badge(t){let b=document.getElementById('__vc_badge__'); if(!b){b=document.createElement('div'); b.id='__vc_badge__'; b.className='__vc_badge__'; document.documentElement.appendChild(b);} b.textContent=t;}
  function clean(t){return (t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim();}
  function abs(h){try{return new URL(h,location.href).href}catch(e){return h||''}}
  function linkList(r){const out=[]; const seen={}; Array.from(r.querySelectorAll('a[href]')).forEach(a=>{const u=abs(a.getAttribute('href')); if(!u||!/^https?:/.test(u)||seen[u])return; seen[u]=1; out.push({url:u,text:clean(a.innerText||a.textContent)});}); return out;}
  function choose(c){let b=c, bs=-1, cur=c; for(let d=0; d<10 && cur && cur!==document.body; d++,cur=cur.parentElement){const links=linkList(cur).length; const t=clean(cur.innerText).length; const r=cur.getBoundingClientRect(); const area=Math.min((r.width||0)*(r.height||0),1000000); const sc=links*100000+Math.min(t,5000)+area/10; if(links>=2 && sc>bs){b=cur;bs=sc;}} return b;}
  function pick(ev){
    ev.preventDefault(); ev.stopPropagation(); css();
    let p=ev.changedTouches?ev.changedTouches[0]:ev;
    let el=document.elementFromPoint(p.clientX,p.clientY);
    if(!el){window.VisualCrawler.onError('선택한 영역을 찾지 못했어.');return false;}
    let box=choose(el); let links=linkList(box);
    document.querySelectorAll('.__vc_list_pick__').forEach(x=>x.classList.remove('__vc_list_pick__'));
    box.classList.add('__vc_list_pick__');
    badge('목록 선택됨 · 링크 '+links.length+'개');
    window.VisualCrawler.onListSelected(JSON.stringify({pageUrl:location.href,pageTitle:document.title||'',count:links.length,links:links,preview:clean(box.innerText).slice(0,700)}));
    document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
    return false;
  }
  document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
  document.addEventListener('click',pick,true); document.addEventListener('touchend',pick,true);
  window.VisualCrawler.onInfo('목록범위 선택 모드: 글 목록 전체를 감싸는 영역을 터치해.');
})();
