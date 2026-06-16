(function(){try{
function C(t){return(t||'').replace(/\u00a0/g,' ').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n').trim()}
function sty(){if(document.getElementById('__vcsty'))return;var s=document.createElement('style');s.id='__vcsty';s.textContent='.__vcpick{outline:4px solid #ffd000!important;outline-offset:2px!important;background:rgba(255,208,0,.12)!important}';document.documentElement.appendChild(s)}
function clr(){document.querySelectorAll('.__vcpick').forEach(e=>e.classList.remove('__vcpick'))}
function ls(r){var o=[];Array.from(r.querySelectorAll('a[href]')).forEach(a=>{try{var h=new URL(a.getAttribute('href'),location.href).href;if(/^https?:/.test(h))o.push(h)}catch(e){}});return Array.from(new Set(o))}
function choose(c){var b=c,bs=-1,cur=c;for(var d=0;d<9&&cur&&cur!==document.body;d++,cur=cur.parentElement){var r=cur.getBoundingClientRect(),l=ls(cur).length,t=C(cur.innerText).length,sc=l*100000+Math.min((r.width||0)*(r.height||0),900000)+Math.min(t,4000);if(l>=2&&sc>bs){b=cur;bs=sc}}return b}
function pick(ev){ev.preventDefault();ev.stopPropagation();var p=ev.changedTouches?ev.changedTouches[0]:ev,c=document.elementFromPoint(p.clientX,p.clientY);if(!c){VC.err('선택 실패');return false}var b=choose(c);clr();b.classList.add('__vcpick');VC.list(JSON.stringify({url:location.href,links:ls(b),preview:C(b.innerText).slice(0,700)}));document.removeEventListener('click',pick,true);document.removeEventListener('touchend',pick,true);return false}
sty();document.addEventListener('click',pick,true);document.addEventListener('touchend',pick,true);
}catch(e){VC.err(String(e&&e.message?e.message:e))}})();
