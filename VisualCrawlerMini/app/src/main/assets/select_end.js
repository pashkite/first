(function(){
  const STYLE_ID='vc_range_style_v13';
  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style'); s.id=STYLE_ID;
    s.textContent='.__vc_range_start__{outline:6px solid #00c853!important;outline-offset:3px!important;background:rgba(0,200,83,.14)!important}.__vc_range_end__{outline:6px solid #ff4081!important;outline-offset:3px!important;background:rgba(255,64,129,.14)!important}.__vc_badge__{position:fixed!important;z-index:2147483647!important;left:12px!important;bottom:12px!important;padding:8px 12px!important;border-radius:999px!important;background:#111!important;color:white!important;font:700 14px sans-serif!important;box-shadow:0 4px 18px rgba(0,0,0,.35)!important}';
    document.documentElement.appendChild(s);
  }
  function badge(t){let b=document.getElementById('__vc_badge__'); if(!b){b=document.createElement('div'); b.id='__vc_badge__'; b.className='__vc_badge__'; document.documentElement.appendChild(b);} b.textContent=t;}
  function clean(t){return (t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim();}
  function esc(s){try{return CSS.escape(s)}catch(e){return String(s).replace(/[^a-zA-Z0-9_-]/g,'\\$&')}}
  function cssPath(el){
    if(el.id && !/^\d+$/.test(el.id)) return '#'+esc(el.id);
    const parts=[]; let cur=el;
    while(cur && cur.nodeType===1 && cur!==document.body){
      let part=cur.tagName.toLowerCase();
      const cls=Array.from(cur.classList||[]).filter(c=>!c.startsWith('__vc_')&&!/^\d+$/.test(c)).slice(0,2);
      if(cls.length) part+='.'+cls.map(esc).join('.');
      let n=1, sib=cur; while((sib=sib.previousElementSibling)!=null){ if(sib.tagName===cur.tagName) n++; }
      part+=':nth-of-type('+n+')'; parts.unshift(part); cur=cur.parentElement;
    }
    return parts.join(' > ');
  }
  function pickBlock(el){
    let cur=el, best=el;
    for(let i=0;i<6 && cur && cur!==document.body;i++,cur=cur.parentElement){
      const t=clean(cur.innerText||cur.textContent);
      if(t.length>0 && t.length<=500){best=cur; break;}
      if(t.length>500 && best) break;
    }
    return best||el;
  }
  function pick(ev){
    ev.preventDefault(); ev.stopPropagation(); css();
    let p=ev.changedTouches?ev.changedTouches[0]:ev;
    let el=document.elementFromPoint(p.clientX,p.clientY);
    if(!el){window.VisualCrawler.onError('본문 끝점을 찾지 못했어.');return false;}
    let box=pickBlock(el);
    document.querySelectorAll('.__vc_range_end__').forEach(x=>x.classList.remove('__vc_range_end__'));
    box.classList.add('__vc_range_end__');
    const pv=clean(box.innerText||box.textContent).slice(0,500);
    badge('본문 끝 선택됨');
    window.VisualCrawler.onRangeEndSelected(cssPath(box), pv);
    document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
    return false;
  }
  document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
  document.addEventListener('click',pick,true); document.addEventListener('touchend',pick,true);
  window.VisualCrawler.onInfo('본문 끝 선택 모드: 본문 마지막 문장 근처를 터치해. 댓글/버튼 위에서 끝내는 게 좋아.');
})();