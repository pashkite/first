(function(){
  const STYLE_ID='vc_range_style_v13_drag';
  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style'); s.id=STYLE_ID;
    s.textContent='.__vc_range_start__{outline:6px solid #00c853!important;outline-offset:3px!important;background:rgba(0,200,83,.14)!important}.__vc_range_end__{outline:6px solid #ff4081!important;outline-offset:3px!important;background:rgba(255,64,129,.14)!important}.__vc_drag_box__{position:absolute!important;z-index:2147483646!important;border:4px solid #00b0ff!important;background:rgba(0,176,255,.12)!important;pointer-events:none!important}.__vc_badge__{position:fixed!important;z-index:2147483647!important;left:12px!important;bottom:12px!important;padding:8px 12px!important;border-radius:999px!important;background:#111!important;color:white!important;font:700 14px sans-serif!important;box-shadow:0 4px 18px rgba(0,0,0,.35)!important}';
    document.documentElement.appendChild(s);
  }
  function badge(t){let b=document.getElementById('__vc_badge__'); if(!b){b=document.createElement('div'); b.id='__vc_badge__'; b.className='__vc_badge__'; document.documentElement.appendChild(b);} b.textContent=t;}
  function clean(t){return (t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim();}
  function esc(s){try{return CSS.escape(s)}catch(e){return String(s).replace(/[^a-zA-Z0-9_-]/g,'\\$&')}}
  function cssPath(el){if(el.id&&!/^\d+$/.test(el.id))return '#'+esc(el.id);const parts=[];let cur=el;while(cur&&cur.nodeType===1&&cur!==document.body){let part=cur.tagName.toLowerCase();const cls=Array.from(cur.classList||[]).filter(c=>!c.startsWith('__vc_')&&!/^\d+$/.test(c)).slice(0,2);if(cls.length)part+='.'+cls.map(esc).join('.');let n=1,sib=cur;while((sib=sib.previousElementSibling)!=null){if(sib.tagName===cur.tagName)n++;}part+=':nth-of-type('+n+')';parts.unshift(part);cur=cur.parentElement;}return parts.join(' > ')}
  function pickBlock(el){let cur=el,best=el;for(let i=0;i<6&&cur&&cur!==document.body;i++,cur=cur.parentElement){const t=clean(cur.innerText||cur.textContent);if(t.length>0&&t.length<=500){best=cur;break;}if(t.length>500&&best)break;}return best||el;}
  function point(ev){let p=(ev.changedTouches&&ev.changedTouches[0])||(ev.touches&&ev.touches[0])||ev;return {x:p.clientX,y:p.clientY};}
  function elemAt(pt){return document.elementFromPoint(pt.x,pt.y);}
  function rect(a,b){let l=Math.min(a.x,b.x),t=Math.min(a.y,b.y),r=Math.max(a.x,b.x),bt=Math.max(a.y,b.y);return {left:l+scrollX,top:t+scrollY,width:r-l,height:bt-t};}
  function draw(r){let box=document.getElementById('__vc_drag_box__');if(!box){box=document.createElement('div');box.id='__vc_drag_box__';box.className='__vc_drag_box__';document.body.appendChild(box);}box.style.left=r.left+'px';box.style.top=r.top+'px';box.style.width=Math.max(2,r.width)+'px';box.style.height=Math.max(2,r.height)+'px';}
  function mark(s,e){document.querySelectorAll('.__vc_range_start__').forEach(x=>x.classList.remove('__vc_range_start__'));document.querySelectorAll('.__vc_range_end__').forEach(x=>x.classList.remove('__vc_range_end__'));s.classList.add('__vc_range_start__');e.classList.add('__vc_range_end__');}
  let startPt=null, dragging=false;
  function down(ev){ev.preventDefault();ev.stopPropagation();css();dragging=true;startPt=point(ev);draw(rect(startPt,startPt));badge('본문 범위 드래그 중...');return false;}
  function move(ev){if(!dragging)return;ev.preventDefault();ev.stopPropagation();draw(rect(startPt,point(ev)));return false;}
  function up(ev){if(!dragging)return;ev.preventDefault();ev.stopPropagation();dragging=false;let endPt=point(ev);let sEl=elemAt(startPt), eEl=elemAt(endPt);if(!sEl||!eEl){window.VisualCrawler.onError('본문 범위를 찾지 못했어.');cleanup();return false;}let s=pickBlock(sEl), e=pickBlock(eEl);mark(s,e);let sp=clean(s.innerText||s.textContent).slice(0,500), ep=clean(e.innerText||e.textContent).slice(0,500);window.VisualCrawler.onRangeStartSelected(cssPath(s), sp);window.VisualCrawler.onRangeEndSelected(cssPath(e), ep);badge('본문 시작/끝 드래그 선택됨');cleanup();return false;}
  function cleanup(){document.removeEventListener('touchstart',down,true);document.removeEventListener('touchmove',move,true);document.removeEventListener('touchend',up,true);document.removeEventListener('mousedown',down,true);document.removeEventListener('mousemove',move,true);document.removeEventListener('mouseup',up,true);}
  cleanup();css();document.addEventListener('touchstart',down,true);document.addEventListener('touchmove',move,true);document.addEventListener('touchend',up,true);document.addEventListener('mousedown',down,true);document.addEventListener('mousemove',move,true);document.addEventListener('mouseup',up,true);
  window.VisualCrawler.onInfo('본문 시작 버튼이 드래그 모드로 바뀌었어. 첫 문장부터 마지막 문장까지 손가락으로 쭉 드래그해. 초록=시작, 분홍=끝.');
})();
