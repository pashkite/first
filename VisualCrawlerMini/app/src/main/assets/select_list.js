(function(){
  const STYLE_ID='vc_select_style';
  function css(){
    if(document.getElementById(STYLE_ID)) return;
    const s=document.createElement('style');
    s.id=STYLE_ID;
    s.textContent='.__vc_pick__{outline:4px solid #ffd400!important;outline-offset:3px!important;background:rgba(255,212,0,.12)!important}';
    document.documentElement.appendChild(s);
  }
  function clean(t){return (t||'').replace(/\s+/g,' ').trim();}
  function abs(h){try{return new URL(h,location.href).href}catch(e){return h||''}}
  function findBox(el){
    let cur=el;
    for(let i=0;i<8 && cur && cur!==document.body;i++){
      if(cur.querySelectorAll && cur.querySelectorAll('a[href]').length>0) return cur;
      cur=cur.parentElement;
    }
    return el;
  }
  function pick(ev){
    ev.preventDefault(); ev.stopPropagation(); css();
    document.querySelectorAll('.__vc_pick__').forEach(x=>x.classList.remove('__vc_pick__'));
    let el=document.elementFromPoint(ev.clientX,ev.clientY);
    if(!el){window.VisualCrawler.onError('선택한 영역을 찾지 못했어.');return false;}
    el=findBox(el); el.classList.add('__vc_pick__');
    const seen={};
    const links=Array.from(el.querySelectorAll('a[href]')).map(a=>({url:abs(a.getAttribute('href')),text:clean(a.innerText||a.textContent)})).filter(x=>x.url && !x.url.startsWith('javascript:')).filter(x=>{if(seen[x.url])return false;seen[x.url]=true;return true;});
    window.VisualCrawler.onListSelected(JSON.stringify({pageUrl:location.href,pageTitle:document.title||'',count:links.length,links:links}));
    document.removeEventListener('click',pick,true);
    document.removeEventListener('touchend',touch,true);
    return false;
  }
  function touch(ev){if(!ev.changedTouches||!ev.changedTouches.length)return;const t=ev.changedTouches[0];pick({preventDefault:()=>ev.preventDefault(),stopPropagation:()=>ev.stopPropagation(),clientX:t.clientX,clientY:t.clientY});}
  document.removeEventListener('click',pick,true);document.removeEventListener('touchend',touch,true);
  document.addEventListener('click',pick,true);document.addEventListener('touchend',touch,true);
  window.VisualCrawler.onInfo('목록영역 선택 모드: 글 목록 전체를 감싸는 영역을 터치해. 선택 즉시 링크를 기억해.');
})();
