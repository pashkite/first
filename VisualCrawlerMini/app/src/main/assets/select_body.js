(function(){
  const STYLE_ID='vc_body_style_v09';
  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style');
    s.id=STYLE_ID;
    s.textContent='.__vc_body_pick__{outline:6px solid #00e676!important;outline-offset:3px!important;background:rgba(0,230,118,.14)!important}.__vc_badge__{position:fixed!important;z-index:2147483647!important;left:12px!important;bottom:12px!important;padding:8px 12px!important;border-radius:999px!important;background:#111!important;color:white!important;font:700 14px sans-serif!important;box-shadow:0 4px 18px rgba(0,0,0,.35)!important}';
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
      if(cls.length) part += '.' + cls.map(esc).join('.');
      let n=1, sib=cur;
      while((sib=sib.previousElementSibling)!=null){ if(sib.tagName===cur.tagName) n++; }
      part += ':nth-of-type('+n+')';
      parts.unshift(part); cur=cur.parentElement;
    }
    return parts.join(' > ');
  }
  function imgInfo(img){
    if(!img) return '';
    const alt=clean(img.getAttribute('alt')||img.getAttribute('title')||'');
    let cap='';
    const fig=img.closest&&img.closest('figure');
    if(fig){const fc=fig.querySelector('figcaption'); if(fc) cap=clean(fc.innerText||fc.textContent);}
    const src=img.currentSrc||img.src||img.getAttribute('src')||'';
    return clean([alt,cap,src?('[이미지] '+src):''].filter(Boolean).join('\n')) || '[이미지: 설명 없음]';
  }
  function preview(el){
    if(!el) return '';
    if(el.tagName==='IMG') return imgInfo(el);
    if(el.tagName==='A'){
      const t=clean(el.innerText||el.textContent);
      if(t) return t;
      const im=el.querySelector('img');
      if(im) return imgInfo(im);
      return el.href ? '[링크] '+el.href : '[링크: 표시 텍스트 없음]';
    }
    const imDirect=el.closest&&el.closest('img');
    if(imDirect) return imgInfo(imDirect);
    const t=clean(el.innerText||el.textContent);
    if(t) return t;
    const im=el.querySelector&&el.querySelector('img');
    if(im) return imgInfo(im);
    return '';
  }
  function textBlock(c){
    let cur=c;
    for(let i=0;i<7&&cur&&cur!==document.body;i++,cur=cur.parentElement){
      const t=clean(cur.innerText||cur.textContent);
      if(t.length>=20) return cur;
    }
    return c;
  }
  function target(c){
    if(!c) return null;
    const a=c.closest&&c.closest('a[href]');
    if(a) return {el:a,note:'링크 안의 보이는 텍스트'};
    const img=(c.tagName==='IMG')?c:(c.closest&&c.closest('img'));
    if(img) return {el:img,note:'이미지 설명/캡션/주소'};
    const pic=c.closest&&c.closest('picture,svg,video,iframe,canvas');
    if(pic) return {el:pic,note:'미디어 요소 텍스트/정보'};
    return {el:textBlock(c),note:'일반 본문 텍스트 블록'};
  }
  function pick(ev){
    ev.preventDefault(); ev.stopPropagation(); css();
    let p=ev.changedTouches?ev.changedTouches[0]:ev;
    let c=document.elementFromPoint(p.clientX,p.clientY);
    if(!c){window.VisualCrawler.onError('본문 요소를 찾지 못했어.');return false;}
    let t=target(c); if(!t||!t.el){window.VisualCrawler.onError('본문 요소를 찾지 못했어.');return false;}
    document.querySelectorAll('.__vc_body_pick__').forEach(x=>x.classList.remove('__vc_body_pick__'));
    t.el.classList.add('__vc_body_pick__');
    const pv=preview(t.el);
    badge('본문 선택됨 · '+t.note);
    window.VisualCrawler.onBodySelected(cssPath(t.el), pv.slice(0,900), t.note);
    document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
    return false;
  }
  document.removeEventListener('click',pick,true); document.removeEventListener('touchend',pick,true);
  document.addEventListener('click',pick,true); document.addEventListener('touchend',pick,true);
  window.VisualCrawler.onInfo('본문요소 선택 모드: 링크는 링크 텍스트, 이미지는 설명/주소, 일반 영역은 텍스트 블록을 선택해.');
})();
