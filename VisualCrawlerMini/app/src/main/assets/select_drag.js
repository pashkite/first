(function(){
  const STYLE_ID='vc_drag_style_v14';
  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style'); s.id=STYLE_ID;
    s.textContent='.__vc_drag_rect__{position:absolute!important;z-index:2147483646!important;border:4px solid #00b0ff!important;background:rgba(0,176,255,.14)!important;box-shadow:0 0 0 99999px rgba(0,0,0,.08)!important;pointer-events:none!important}.__vc_badge__{position:fixed!important;z-index:2147483647!important;left:12px!important;bottom:12px!important;padding:8px 12px!important;border-radius:999px!important;background:#111!important;color:white!important;font:700 14px sans-serif!important;box-shadow:0 4px 18px rgba(0,0,0,.35)!important}';
    document.documentElement.appendChild(s);
  }
  function badge(t){let b=document.getElementById('__vc_badge__'); if(!b){b=document.createElement('div'); b.id='__vc_badge__'; b.className='__vc_badge__'; document.documentElement.appendChild(b);} b.textContent=t;}
  function clean(t){return (t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim();}
  function pt(ev){let p=(ev.changedTouches&&ev.changedTouches[0])||(ev.touches&&ev.touches[0])||ev; return {x:p.clientX,y:p.clientY};}
  function rectFrom(a,b){let l=Math.min(a.x,b.x), t=Math.min(a.y,b.y), r=Math.max(a.x,b.x), bo=Math.max(a.y,b.y); return {left:l+scrollX,top:t+scrollY,right:r+scrollX,bottom:bo+scrollY,width:r-l,height:bo-t};}
  function place(el,r){el.style.left=r.left+'px'; el.style.top=r.top+'px'; el.style.width=Math.max(2,r.right-r.left)+'px'; el.style.height=Math.max(2,r.bottom-r.top)+'px';}
  function inter(a,b){return !(a.right<b.left||a.left>b.right||a.bottom<b.top||a.top>b.bottom);}
  function visible(el){try{let s=getComputedStyle(el), r=el.getBoundingClientRect(); return s.display!=='none'&&s.visibility!=='hidden'&&r.width>1&&r.height>1;}catch(e){return false;}}
  function docRect(el){let r=el.getBoundingClientRect(); return {left:r.left+scrollX,top:r.top+scrollY,right:r.right+scrollX,bottom:r.bottom+scrollY,width:r.width,height:r.height};}
  function preview(R){
    const bad='script,style,noscript,iframe,canvas,svg,nav,header,footer,form,input,button,select,textarea';
    const els=Array.from(document.querySelectorAll('p,pre,li,img,div,section,article,main'));
    let parts=[];
    for(const el of els){
      if(parts.length>30) break;
      if(el.matches(bad)||el.closest('nav,header,footer,form')) continue;
      if(!visible(el)) continue;
      if(!inter(docRect(el),R)) continue;
      let tag=(el.tagName||'').toLowerCase();
      let txt=tag==='img' ? (el.getAttribute('alt')||el.getAttribute('title')||'[이미지]') : clean(el.innerText||el.textContent);
      if(txt.length<8 && tag!=='img') continue;
      parts.push(txt.slice(0,180));
    }
    return clean(parts.join('\n'));
  }
  let start=null, box=null, down=false;
  function begin(ev){ev.preventDefault(); ev.stopPropagation(); css(); down=true; start=pt(ev); if(!box){box=document.createElement('div'); box.className='__vc_drag_rect__'; document.body.appendChild(box);} let r=rectFrom(start,start); place(box,r); badge('드래그 중...'); return false;}
  function move(ev){if(!down)return; ev.preventDefault(); ev.stopPropagation(); let r=rectFrom(start,pt(ev)); place(box,r); badge('본문영역 드래그 · '+Math.round(r.width)+'×'+Math.round(r.height)); return false;}
  function end(ev){if(!down)return; ev.preventDefault(); ev.stopPropagation(); down=false; let r=rectFrom(start,pt(ev)); if(r.width<20||r.height<20){badge('영역이 너무 작아. 다시 드래그해.'); return false;} place(box,r); let pv=preview(r).slice(0,900); badge('본문영역 선택됨 · 후보 미리보기 '+pv.length+'자'); cleanup(); window.VisualCrawler.onDragSelected(JSON.stringify({pageUrl:location.href,left:Math.round(r.left),top:Math.round(r.top),right:Math.round(r.right),bottom:Math.round(r.bottom),width:Math.round(r.width),height:Math.round(r.height),preview:pv})); return false;}
  function cleanup(){document.removeEventListener('touchstart',begin,true);document.removeEventListener('touchmove',move,true);document.removeEventListener('touchend',end,true);document.removeEventListener('mousedown',begin,true);document.removeEventListener('mousemove',move,true);document.removeEventListener('mouseup',end,true);}
  cleanup(); css(); document.addEventListener('touchstart',begin,true);document.addEventListener('touchmove',move,true);document.addEventListener('touchend',end,true);document.addEventListener('mousedown',begin,true);document.addEventListener('mousemove',move,true);document.addEventListener('mouseup',end,true);
  window.VisualCrawler.onInfo('본문영역 드래그 모드: 파란 사각형으로 가져올 화면 영역을 직접 그려. OCR이 아니라 그 영역 안의 웹 텍스트/이미지를 가져와.');
})();
