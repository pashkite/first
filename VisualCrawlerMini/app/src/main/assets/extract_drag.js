function VisualCrawlerExtractDrag(rectString){
  function C(t){return (t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim()}
  function A(u){try{return new URL(u,location.href).href}catch(e){return u||''}}
  function esc(s){return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;')}
  function R(el){var r=el.getBoundingClientRect(); return {left:r.left+scrollX,top:r.top+scrollY,right:r.right+scrollX,bottom:r.bottom+scrollY,width:r.width,height:r.height}}
  function inter(a,b){return !(a.right<b.left||a.left>b.right||a.bottom<b.top||a.top>b.bottom)}
  function visible(el){try{var s=getComputedStyle(el),r=el.getBoundingClientRect();return s.display!=='none'&&s.visibility!=='hidden'&&r.width>1&&r.height>1}catch(e){return false}}
  function bad(el){if(!el||!el.matches)return true; if(el.matches('script,style,noscript,iframe,canvas,svg,nav,header,footer,form,input,button,select,textarea'))return true; if(el.closest&&el.closest('nav,header,footer,form'))return true; var cl=((el.className||'')+' '+(el.id||'')).toLowerCase(); if(/comment|reply|toolbar|breadcrumb|pagination|bookmark|login|advert|ads/.test(cl))return true; return false}
  function cleanNode(n){try{n.querySelectorAll('script,style,noscript,iframe,canvas,svg,nav,header,footer,form,input,button,select,textarea,[class*=comment],[id*=comment],[class*=reply],[id*=reply],[class*=toolbar],[id*=toolbar],[class*=breadcrumb],[id*=breadcrumb],[class*=pagination],[id*=pagination],[class*=bookmark],[id*=bookmark],[class*=login],[id*=login]').forEach(function(x){x.remove()})}catch(e){}
    try{n.querySelectorAll('a').forEach(function(a){var img=a.querySelector&&a.querySelector('img'); if(img&&!C(a.innerText||a.textContent)){var im=img.cloneNode(true); a.replaceWith(im)}else{var sp=document.createElement('span'); sp.textContent=C(a.innerText||a.textContent); a.replaceWith(sp)}})}catch(e){}
    try{n.querySelectorAll('img').forEach(function(im){var src=A(im.currentSrc||im.src||im.getAttribute('src')||''); if(src)im.setAttribute('src',src); im.removeAttribute('srcset'); im.removeAttribute('sizes'); im.setAttribute('style','max-width:100%;height:auto;display:block;margin:12px 0;')})}catch(e){}
    return n
  }
  function score(el,rect){var tag=(el.tagName||'').toLowerCase(); var t=C(el.innerText||el.textContent); var imgs=tag==='img'?1:(el.querySelectorAll?el.querySelectorAll('img').length:0); var a=R(el); var area=Math.max(1,a.width*a.height); var rectArea=Math.max(1,(rect.right-rect.left)*(rect.bottom-rect.top)); var link=el.querySelectorAll?el.querySelectorAll('a').length:0; var ctrl=el.querySelectorAll?el.querySelectorAll('button,input,select,textarea').length:0; var ui=0; ['댓글','로그인','회원가입','이전화','다음화','책갈피','목록','글자','불러오는 중','좌우로 드래그'].forEach(function(w){if(t.indexOf(w)>=0)ui++}); var base=t.length+imgs*100-link*25-ctrl*80-ui*160; if(tag==='p'||tag==='pre'||tag==='li')base+=80; if(area>rectArea*3)base-=300; return base}
  function order(a,b){var p=a.compareDocumentPosition(b); if(p&Node.DOCUMENT_POSITION_FOLLOWING)return -1; if(p&Node.DOCUMENT_POSITION_PRECEDING)return 1; return 0}
  var rect=JSON.parse(rectString||'{}'); rect.left=+rect.left||0; rect.top=+rect.top||0; rect.right=+rect.right||0; rect.bottom=+rect.bottom||0;
  var raw=Array.from(document.querySelectorAll('p,pre,li,img,div,section,article,main'));
  var cand=[];
  raw.forEach(function(el){if(bad(el)||!visible(el))return; var rr=R(el); if(!inter(rr,rect))return; var tag=(el.tagName||'').toLowerCase(); var t=C(el.innerText||el.textContent); if(tag!=='img'&&t.length<8)return; if(score(el,rect)<-50)return; cand.push(el)});
  cand.sort(order);
  var selected=[];
  cand.forEach(function(el){
    var contained=selected.some(function(s){return s.contains(el)&&R(s).width*R(s).height>R(el).width*R(el).height*1.4});
    if(contained){ selected=selected.filter(function(s){return !(s.contains(el)&&R(s).width*R(s).height>R(el).width*R(el).height*1.4)}); }
    var parentAlready=selected.some(function(s){return s!==el && s.contains(el)});
    if(!parentAlready) selected.push(el);
  });
  if(selected.length===0){selected=[document.body]}
  var wrap=document.createElement('div'); selected.slice(0,80).forEach(function(el){wrap.appendChild(el.cloneNode(true))}); cleanNode(wrap);
  var html=wrap.innerHTML;
  var d=document.createElement('div'); d.innerHTML=html; var text=C(d.innerText||d.textContent||'');
  var imgs=[]; Array.from(d.querySelectorAll('img')).forEach(function(im){var src=A(im.getAttribute('src')||''); if(src&&/^https?:/.test(src)&&imgs.indexOf(src)<0)imgs.push(src)});
  return JSON.stringify({title:document.title||'',url:location.href,text:text,html:html,images:imgs,count:selected.length,ready:document.readyState});
}
VisualCrawlerExtractDrag