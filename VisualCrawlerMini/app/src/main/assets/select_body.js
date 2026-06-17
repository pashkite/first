(function(){
  const STYLE_ID='vc_body_drag_style_v11';
  const PICK='__vc_body_pick__';
  const HOVER='__vc_body_drag_hover__';
  const BADGE='__vc_badge__';
  const MODE='__vc_body_drag_mode__';

  if(window.__vcBodyDragCleanup){
    try{window.__vcBodyDragCleanup(true);}catch(e){}
  }

  let active=false;
  let startEl=null;
  let currentEl=null;
  let lastCandidate=null;
  let usingPointer=!!window.PointerEvent;

  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style');
    s.id=STYLE_ID;
    s.textContent='.__vc_body_pick__{outline:6px solid #00e676!important;outline-offset:3px!important;background:rgba(0,230,118,.14)!important}.__vc_body_drag_hover__{outline:5px dashed #00e676!important;outline-offset:3px!important;background:rgba(0,230,118,.10)!important}.__vc_badge__{position:fixed!important;z-index:2147483647!important;left:12px!important;bottom:12px!important;max-width:calc(100vw - 24px)!important;padding:9px 13px!important;border-radius:999px!important;background:#111!important;color:white!important;font:700 14px sans-serif!important;box-shadow:0 4px 18px rgba(0,0,0,.35)!important;pointer-events:none!important}html.__vc_body_drag_mode__,body.__vc_body_drag_mode__{overscroll-behavior:contain!important}.__vc_body_drag_mode__ *{-webkit-user-select:none!important;user-select:none!important}';
    document.documentElement.appendChild(s);
  }
  function addMode(){
    document.documentElement.classList.add(MODE);
    if(document.body) document.body.classList.add(MODE);
  }
  function removeMode(){
    document.documentElement.classList.remove(MODE);
    if(document.body) document.body.classList.remove(MODE);
  }
  function badge(t){
    let b=document.getElementById(BADGE);
    if(!b){b=document.createElement('div'); b.id=BADGE; b.className=BADGE; document.documentElement.appendChild(b);}
    b.textContent=t;
  }
  function clean(t){return (t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim();}
  function esc(s){try{return CSS.escape(s)}catch(e){return String(s).replace(/[^a-zA-Z0-9_-]/g,'\\$&')}}
  function isOwnUi(el){return !!(el && el.closest && el.closest('#'+BADGE));}
  function clearHover(){document.querySelectorAll('.'+HOVER).forEach(x=>x.classList.remove(HOVER));}
  function clearPick(){document.querySelectorAll('.'+PICK).forEach(x=>x.classList.remove(PICK));}

  function cssPath(el){
    if(!el || el.nodeType!==1) return 'body';
    if(el===document.body) return 'body';
    if(el===document.documentElement) return 'html';
    if(el.id && !/^\d+$/.test(el.id) && !el.id.startsWith('__vc_')) return '#'+esc(el.id);
    const parts=[];
    let cur=el;
    while(cur && cur.nodeType===1){
      if(cur===document.body){parts.unshift('body'); break;}
      let part=cur.tagName.toLowerCase();
      const cls=Array.from(cur.classList||[]).filter(c=>!c.startsWith('__vc_')&&!/^\d+$/.test(c)&&c.length<=48).slice(0,2);
      if(cls.length) part+='.'+cls.map(esc).join('.');
      let n=1, sib=cur;
      while((sib=sib.previousElementSibling)!=null){if(sib.tagName===cur.tagName)n++;}
      part+=':nth-of-type('+n+')';
      parts.unshift(part);
      cur=cur.parentElement;
    }
    return parts.join(' > ') || 'body';
  }

  function point(ev){
    if(ev.touches && ev.touches.length) return ev.touches[0];
    if(ev.changedTouches && ev.changedTouches.length) return ev.changedTouches[0];
    return ev;
  }
  function elementAt(ev){
    const p=point(ev);
    if(!p || typeof p.clientX!=='number' || typeof p.clientY!=='number') return null;
    let el=document.elementFromPoint(p.clientX,p.clientY);
    if(isOwnUi(el)) return null;
    return el;
  }
  function ancestors(el){
    const arr=[];
    for(let cur=el;cur&&cur.nodeType===1;cur=cur.parentElement) arr.push(cur);
    return arr;
  }
  function commonAncestor(a,b){
    if(!a || !b) return a||b||document.body;
    const aa=ancestors(a);
    let cur=b;
    while(cur && cur.nodeType===1){
      if(aa.indexOf(cur)>=0) return cur;
      cur=cur.parentElement;
    }
    return document.body;
  }
  function isInline(el){
    if(!el || el===document.body) return false;
    const tag=(el.tagName||'').toLowerCase();
    if(/^(span|b|strong|em|i|small|mark|code|time|label)$/.test(tag)) return true;
    try{return getComputedStyle(el).display.indexOf('inline')===0;}catch(e){return false;}
  }
  function hasMedia(el){return !!(el && el.querySelector && el.querySelector('img,video,iframe,picture,figure')) || (el && /^(img|video|iframe|picture|figure)$/i.test(el.tagName||''));}
  function textLen(el){return clean(el?(el.innerText||el.textContent):'').length;}
  function betterBlock(el){
    if(!el) return document.body;
    while(isInline(el) && el.parentElement) el=el.parentElement;
    let cur=el;
    for(let i=0;i<6 && cur && cur!==document.body;i++,cur=cur.parentElement){
      const tag=(cur.tagName||'').toLowerCase();
      const len=textLen(cur);
      if(hasMedia(cur) || len>=20 || /^(article|main|section|div|li|td|blockquote|p|figure)$/.test(tag)) return cur;
    }
    return el||document.body;
  }
  function candidate(){
    const c=commonAncestor(startEl,currentEl||startEl);
    return betterBlock(c);
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
    const text=clean(el.innerText||el.textContent);
    if(text) return text;
    const im=el.querySelector&&el.querySelector('img');
    if(im) return imgInfo(im);
    return '';
  }
  function paint(el,finalPick){
    if(!el) return;
    clearHover();
    if(finalPick){clearPick(); el.classList.add(PICK);} else {el.classList.add(HOVER);}
  }
  function prevent(ev){
    try{ev.preventDefault(); ev.stopPropagation(); if(ev.stopImmediatePropagation) ev.stopImmediatePropagation();}catch(e){}
    return false;
  }
  function update(ev){
    const el=elementAt(ev);
    if(el) currentEl=el;
    lastCandidate=candidate();
    if(lastCandidate){paint(lastCandidate,false); badge('본문 드래그 선택 중 · 손을 떼면 초록 영역을 기억함');}
  }
  function start(ev){
    const el=elementAt(ev);
    if(!el){window.VisualCrawler.onError('드래그 시작 요소를 찾지 못했어.'); return prevent(ev);}
    active=true;
    startEl=el;
    currentEl=el;
    clearPick();
    addMode();
    update(ev);
    return prevent(ev);
  }
  function move(ev){
    if(!active) return;
    update(ev);
    return prevent(ev);
  }
  function end(ev){
    if(!active) return;
    update(ev);
    active=false;
    removeMode();
    const el=lastCandidate || candidate();
    clearHover();
    if(!el){window.VisualCrawler.onError('본문 요소를 찾지 못했어.'); return prevent(ev);}
    paint(el,true);
    const sel=cssPath(el);
    const pv=preview(el).slice(0,1200);
    const note='손가락 드래그 선택 · 시작/끝점 공통 본문 블록';
    badge('본문 선택됨 · 드래그 블록');
    window.VisualCrawler.onBodySelected(sel,pv,note);
    cleanup(false);
    return prevent(ev);
  }
  function cancel(ev){
    active=false;
    clearHover();
    removeMode();
    badge('본문 드래그 선택 취소됨');
    cleanup(false);
    return prevent(ev||window.event);
  }

  const opts={capture:true,passive:false};
  function on(type,fn){document.addEventListener(type,fn,opts);}
  function off(type,fn){document.removeEventListener(type,fn,opts);}
  function cleanup(removeVisual){
    if(usingPointer){off('pointerdown',start);off('pointermove',move);off('pointerup',end);off('pointercancel',cancel);}
    else{off('touchstart',start);off('touchmove',move);off('touchend',end);off('touchcancel',cancel);off('mousedown',start);off('mousemove',move);off('mouseup',end);}
    document.removeEventListener('keydown',key,true);
    removeMode();
    clearHover();
    if(removeVisual) clearPick();
  }
  function key(ev){if(ev.key==='Escape') cancel(ev);}
  window.__vcBodyDragCleanup=cleanup;

  css();
  clearHover();
  clearPick();
  if(usingPointer){on('pointerdown',start);on('pointermove',move);on('pointerup',end);on('pointercancel',cancel);}
  else{on('touchstart',start);on('touchmove',move);on('touchend',end);on('touchcancel',cancel);on('mousedown',start);on('mousemove',move);on('mouseup',end);}
  document.addEventListener('keydown',key,true);
  badge('본문 드래그 선택 모드 · 시작부터 끝까지 끌고 손을 떼');
  window.VisualCrawler.onInfo('본문요소 드래그 선택 모드: 본문 시작점에서 끝점까지 손가락으로 끌고 손을 떼면 공통 본문 블록을 기억해.');
})();
