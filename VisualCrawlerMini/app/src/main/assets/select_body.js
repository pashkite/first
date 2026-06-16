(function(){
  const STYLE_ID='vc_body_style';
  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style');
    s.id=STYLE_ID;
    s.textContent='.__vc_body_pick__{outline:4px solid #ffd400!important;outline-offset:3px!important;background:rgba(255,212,0,.12)!important}';
    document.documentElement.appendChild(s);
  }
  function clean(t){return (t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim();}
  function esc(s){try{return CSS.escape(s)}catch(e){return String(s).replace(/[^a-zA-Z0-9_-]/g,'\\$&')}}
  function cssPath(el){
    if(el.id) return '#'+esc(el.id);
    const parts=[]; let cur=el;
    while(cur && cur.nodeType===1 && cur!==document.body){
      let part=cur.tagName.toLowerCase();
      const cls=Array.from(cur.classList||[]).filter(c=>!c.startsWith('__vc_')).slice(0,2);
      if(cls.length) part += '.' + cls.map(esc).join('.');
      else {
        let n=1, sib=cur;
        while((sib=sib.previousElementSibling)!=null){ if(sib.tagName===cur.tagName) n++; }
        part += ':nth-of-type('+n+')';
      }
      parts.unshift(part);
      cur=cur.parentElement;
    }
    return parts.join(' > ');
  }
  function pick(ev){
    ev.preventDefault(); ev.stopPropagation(); css();
    document.querySelectorAll('.__vc_body_pick__').forEach(x=>x.classList.remove('__vc_body_pick__'));
    const el=document.elementFromPoint(ev.clientX,ev.clientY);
    if(!el){window.VisualCrawler.onError('본문 영역을 찾지 못했어.');return false;}
    el.classList.add('__vc_body_pick__');
    window.VisualCrawler.onBodySelected(cssPath(el), clean(el.innerText||el.textContent));
    document.removeEventListener('click',pick,true);
    document.removeEventListener('touchend',touch,true);
    return false;
  }
  function touch(ev){if(!ev.changedTouches||!ev.changedTouches.length)return;const t=ev.changedTouches[0];pick({preventDefault:()=>ev.preventDefault(),stopPropagation:()=>ev.stopPropagation(),clientX:t.clientX,clientY:t.clientY});}
  document.removeEventListener('click',pick,true);document.removeEventListener('touchend',touch,true);
  document.addEventListener('click',pick,true);document.addEventListener('touchend',touch,true);
  window.VisualCrawler.onInfo('본문영역 선택 모드: 샘플 글에서 가져올 본문 영역을 터치해.');
})();
