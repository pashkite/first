(function(){
  const STYLE_ID='vc_select_style_v11';
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
  function norm(h){try{let u=new URL(h,location.href); u.hash=''; let s=u.href; if(s.endsWith('/') && u.pathname!=='/') s=s.slice(0,-1); return s;}catch(e){return h||''}}
  function linkText(a){let t=clean(a.innerText||a.textContent||a.getAttribute('aria-label')||a.getAttribute('title')||''); if(t)return t; let im=a.querySelector&&a.querySelector('img'); if(im)return clean(im.getAttribute('alt')||im.getAttribute('title')||''); return '';}
  function rawLinks(r){const out=[]; Array.from(r.querySelectorAll('a[href]')).forEach(a=>{let h=a.getAttribute('href')||''; if(!h||h[0]==='#'||/^javascript:/i.test(h)||/^mailto:/i.test(h))return; const u=norm(abs(h)); if(!u||!/^https?:/.test(u))return; out.push({url:u,text:linkText(a)});}); return out;}
  function uniqueLinks(r){const out=[]; const seen={}; rawLinks(r).forEach(x=>{if(seen[x.url])return; seen[x.url]=1; out.push(x);}); return out;}
  function choose(c){
    let best=null, bestScore=-1, cur=c;
    for(let d=0; d<12 && cur && cur!==document.documentElement; d++,cur=cur.parentElement){
      if(cur===document.body && best) break;
      const links=uniqueLinks(cur);
      if(links.length<2) continue;
      const raw=rawLinks(cur).length;
      const textLen=clean(cur.innerText||cur.textContent).length;
      const r=cur.getBoundingClientRect();
      const area=Math.max(1,(r.width||0)*(r.height||0));
      const density=links.length/area*1000000;
      let navPenalty=0;
      const tag=(cur.tagName||'').toLowerCase();
      if(tag==='body'||tag==='html') navPenalty+=900;
      if(cur.closest&&cur.closest('nav,header,footer')) navPenalty+=700;
      const duplicatePenalty=Math.max(0,raw-links.length)*2;
      const hugePenalty=Math.max(0,area-650000)/1200;
      const score=links.length*110 + density*45 + Math.min(textLen,2500)/30 - hugePenalty - duplicatePenalty - navPenalty;
      if(score>bestScore){best=cur; bestScore=score;}
    }
    return best||c;
  }
  function pick(ev){
    ev.preventDefault(); ev.stopPropagation(); css();
    let p=ev.changedTouches?ev.changedTouches[0]:ev;
    let el=document.elementFromPoint(p.clientX,p.clientY);
    if(!el){window.VisualCrawler.onError('선택한 영역을 찾지 못했어.');return false;}
    let box=choose(el); let links=uniqueLinks(box); let raw=rawLinks(box).length;
    document.querySelectorAll('.__vc_list_pick__').forEach(x=>x.classList.remove('__vc_list_pick__'));
    box.classList.add('__vc_list_pick__');
    badge('목록 선택됨 · 후보 '+raw+'개 · 중복제거 '+links.length+'개');
    window.VisualCrawler.onListSelected(JSON.stringify({pageUrl:location.href,pageTitle:document.title||'',rawCount:raw,count:links.length,links:links,preview:clean(box.innerText).slice(0,700)}));
    document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
    return false;
  }
  document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
  document.addEventListener('click',pick,true); document.addEventListener('touchend',pick,true);
  window.VisualCrawler.onInfo('목록범위 선택 모드: v0.11은 큰 페이지 전체보다 링크 밀도가 높은 목록 박스를 우선 선택해.');
})();
