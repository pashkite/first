package com.example.visualcrawlerv05;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  WebView w; EditText url; TextView stat,out;
  ArrayList<String> links=new ArrayList<>(), blocks=new ArrayList<>();
  String bodySel="", listUrl="", last="";
  boolean running=false; int idx=0; Handler h=new Handler(Looper.getMainLooper());

  @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
  public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setFitsSystemWindows(true); root.setPadding(dp(10),dp(44),dp(10),dp(10));
    TextView title=new TextView(this); title.setText("Visual Crawler v0.8 · 선택 강조판"); title.setTextSize(15); title.setPadding(4,0,4,6);
    LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
    url=new EditText(this); url.setSingleLine(true); url.setHint("https://example.com"); url.setText("https://example.com"); url.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    Button go=B("이동"), help=B("?"); go.setOnClickListener(v->go()); help.setOnClickListener(v->help()); top.addView(url); top.addView(go); top.addView(help);
    LinearLayout r1=new LinearLayout(this); r1.setOrientation(LinearLayout.HORIZONTAL);
    Button list=B("목록범위 선택"), body=B("본문영역 선택"), start=B("전체수집 시작");
    list.setOnClickListener(v->selList()); body.setOnClickListener(v->selBody()); start.setOnClickListener(v->start()); r1.addView(list,W()); r1.addView(body,W()); r1.addView(start,W());
    LinearLayout r2=new LinearLayout(this); r2.setOrientation(LinearLayout.HORIZONTAL);
    Button save=B("다운로드 저장"), copy=B("결과 복사"), reset=B("초기화"); save.setOnClickListener(v->save()); copy.setOnClickListener(v->copy()); reset.setOnClickListener(v->reset()); r2.addView(save,W()); r2.addView(copy,W()); r2.addView(reset,W());
    stat=new TextView(this); stat.setTextSize(13); stat.setPadding(8,8,8,8); st("v0.8: 선택하면 페이지 안에 굵은 테두리와 라벨이 남아.");
    w=new WebView(this); w.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
    WebSettings s=w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadsImagesAutomatically(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    w.addJavascriptInterface(new Bridge(),"VC"); w.setWebChromeClient(new WebChromeClient());
    w.setWebViewClient(new WebViewClient(){
      public void onPageFinished(WebView v,String u){super.onPageFinished(v,u); url.setText(u); if(running)h.postDelayed(()->grab(),1300);} 
      public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err){ if(Build.VERSION.SDK_INT>=23 && req!=null && req.isForMainFrame()) st("페이지 로드 실패: "+err.getDescription()+". v0.4 방식 WebView 유지 중."); }
      @SuppressWarnings("deprecation") public void onReceivedError(WebView v,int code,String desc,String failingUrl){ st("페이지 로드 실패: "+desc+". v0.4 방식 WebView 유지 중."); }
    });
    ScrollView sv=new ScrollView(this); sv.setLayoutParams(new LinearLayout.LayoutParams(-1,250)); out=new TextView(this); out.setTextSize(13); out.setPadding(8,8,8,8); out.setText("결과가 여기에 표시돼.\n\n선택하면 노란색=목록, 초록색=본문으로 강조돼."); sv.addView(out);
    root.addView(title); root.addView(top); root.addView(r1); root.addView(r2); root.addView(stat); root.addView(w); root.addView(sv); setContentView(root); w.loadUrl(url.getText().toString());
  }

  int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);} Button B(String t){Button b=new Button(this);b.setText(t);b.setTextSize(12);return b;} LinearLayout.LayoutParams W(){return new LinearLayout.LayoutParams(0,-2,1);} void toast(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
  void st(String m){stat.setText(m+"\n상태: 목록 "+links.size()+"개 기억 / 본문 "+(bodySel.length()>0?"선택됨":"미선택")+(running?" / 수집 중 "+idx+"/"+links.size():""));}
  void go(){String u=url.getText().toString().trim(); if(u.length()==0)return; if(!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u; w.loadUrl(u);} 
  void selList(){if(running){toast("수집 중");return;} w.evaluateJavascript(JS_LIST,null); st("노란색으로 남길 글 목록 전체 영역을 터치해.");}
  void selBody(){if(running){toast("수집 중");return;} w.evaluateJavascript(JS_BODY,null); st("초록색으로 남길 글 본문 영역을 터치해.");}
  void start(){if(links.isEmpty()){toast("목록범위 먼저 선택");return;} if(bodySel.length()==0){toast("본문영역 먼저 선택");return;} blocks.clear(); idx=0; running=true; last=""; String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.KOREA).format(new Date()); blocks.add("Visual Crawler 수집 결과\n생성: "+stamp+"\n목록 페이지: "+listUrl+"\n총 링크: "+links.size()+"개\n본문 선택자: "+bodySel+"\n\n==============================\n"); st("전체수집 시작: 0 / "+links.size()); w.loadUrl(links.get(0));}
  void grab(){if(!running||idx>=links.size())return; String q=JSONObject.quote(bodySel); String js="(function(){function C(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}var e=null;try{e=document.querySelector("+q+")}catch(x){}return JSON.stringify({title:document.title||'',url:location.href,text:e?C(e.innerText||e.textContent):''})})()"; w.evaluateJavascript(js,v->got(v));}
  void got(String v){try{if(v==null)v="{}"; if(v.startsWith("\"")&&v.endsWith("\""))v=new JSONArray("["+v+"]").getString(0); JSONObject o=new JSONObject(v); String title=o.optString("title","글 "+(idx+1)); String text=o.optString("text",""); String u=o.optString("url",links.get(idx)); String block="["+(idx+1)+"/"+links.size()+"] "+title+"\nURL: "+u+"\n본문 글자수: "+text.length()+"\n\n"+(text.length()>0?text:"[본문을 찾지 못했어. 본문영역을 다시 선택해봐.]")+"\n\n------------------------------\n"; blocks.add(block); idx++; last=join(); out.setText("수집 중: "+idx+" / "+links.size()+"\n\n"+last); if(idx<links.size()){st("수집 중: "+idx+" / "+links.size()); w.loadUrl(links.get(idx));}else{running=false; st("수집 완료. 파일 첫 부분에 링크 수와 본문 선택자가 기록돼."); toast("수집 완료");}}catch(Exception e){running=false;st("수집 오류: "+e.getMessage());}}
  String join(){StringBuilder sb=new StringBuilder();for(String x:blocks)sb.append(x).append('\n');return sb.toString().trim();}
  void copy(){String t=last.length()>0?last:out.getText().toString(); ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("visual crawler",t)); toast("복사 완료");}
  void save(){String t=last.length()>0?last:out.getText().toString(); if(t.trim().length()==0){toast("저장할 결과 없음");return;} String name="visual_crawler_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date())+".txt"; try{ if(Build.VERSION.SDK_INT>=29){ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"text/plain");cv.put(MediaStore.Downloads.IS_PENDING,1);Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);OutputStream os=getContentResolver().openOutputStream(uri);os.write(t.getBytes(StandardCharsets.UTF_8));os.close();cv.clear();cv.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(uri,cv,null,null);}else{File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);d.mkdirs();FileOutputStream f=new FileOutputStream(new File(d,name));f.write(t.getBytes(StandardCharsets.UTF_8));f.close();} st("저장 완료: Download/"+name+". 파일 상단에서 링크 수/선택자 확인 가능."); toast("저장 완료"); }catch(Exception e){ st("저장 실패: "+e.getMessage()); toast("저장 실패"); }}
  void reset(){ running=false; links.clear(); bodySel=""; listUrl=""; last=""; blocks.clear(); out.setText("초기화 완료. 선택 강조도 새 페이지에서 다시 잡아줘."); st("초기화 완료. 목록범위 선택부터 시작해."); }
  void help(){new AlertDialog.Builder(this).setTitle("사용법").setMessage("v0.8 변경점\n\n· 상단에 여백을 넣어서 알림창과 URL 입력창이 덜 겹치게 했어.\n· 목록범위 선택: 노란색 테두리와 '목록 선택됨' 라벨이 남아.\n· 본문영역 선택: 초록색 테두리와 '본문 선택됨' 라벨이 남아.\n· 저장 파일 상단에는 목록 페이지, 링크 개수, 본문 선택자가 기록돼.\n\n사용 흐름\n1. 목록 페이지에서 [목록범위 선택]\n2. 노란색으로 목록 전체 선택\n3. 글 하나를 열고 [본문영역 선택]\n4. 초록색으로 본문 선택\n5. [전체수집 시작] 후 [다운로드 저장]").setPositiveButton("확인",null).show();}

  public class Bridge{
    @JavascriptInterface public void list(String json){ runOnUiThread(()->{try{JSONObject o=new JSONObject(json); JSONArray a=o.getJSONArray("links"); LinkedHashSet<String> set=new LinkedHashSet<>(); for(int i=0;i<a.length();i++){String x=a.optString(i); if(x.startsWith("http"))set.add(x);} links.clear(); links.addAll(set); listUrl=o.optString("url",w.getUrl()); out.setText("목록범위 선택 완료\n\n기억한 글 링크: "+links.size()+"개\n목록 페이지: "+listUrl+"\n\n선택 영역 미리보기:\n"+o.optString("preview")+"\n\n노란색 테두리가 선택한 목록이야. 글 내부로 들어가도 링크 목록은 앱이 기억해."); st("목록 "+links.size()+"개 기억됨. 노란색 테두리가 선택 범위야.");}catch(Exception e){st("목록 처리 오류: "+e.getMessage());}});}
    @JavascriptInterface public void body(String sel,String preview){ runOnUiThread(()->{ bodySel=sel; out.setText("본문영역 선택 완료\n\n본문 선택자:\n"+sel+"\n\n본문 미리보기:\n"+preview+"\n\n초록색 테두리가 선택한 본문이야. 이제 전체수집 시작."); st("본문영역 기억됨. 초록색 테두리가 선택 범위야."); });}
    @JavascriptInterface public void err(String m){ runOnUiThread(()->st("오류: "+m)); }
  }
  public void onBackPressed(){ if(running){toast("수집 중");return;} if(w.canGoBack())w.goBack(); else super.onBackPressed(); }

  static final String COMMON="function C(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}\n"+"function css(e){if(!e)return'';if(e.id&&!/^\\d+$/.test(e.id))return'#'+CSS.escape(e.id);var p=[];while(e&&e!==document.body){var n=e.tagName.toLowerCase();var cs=Array.from(e.classList||[]).filter(c=>c&&!c.startsWith('__vc')&&!/^\\d+$/.test(c)).slice(0,2);if(cs.length)n+='.'+cs.map(CSS.escape).join('.');var pa=e.parentElement;if(pa){var s=Array.from(pa.children).filter(x=>x.tagName===e.tagName);if(s.length>1)n+=':nth-of-type('+(s.indexOf(e)+1)+')'}p.unshift(n);e=pa}return p.join(' > ')}\n"+"function sty(){if(document.getElementById('__vcsty'))return;var s=document.createElement('style');s.id='__vcsty';s.textContent='.__vcList{outline:6px solid #ffd000!important;outline-offset:3px!important;background:rgba(255,208,0,.16)!important}.__vcBody{outline:6px solid #00e676!important;outline-offset:3px!important;background:rgba(0,230,118,.14)!important}.__vcBadge{position:fixed!important;z-index:2147483647!important;left:12px!important;bottom:12px!important;padding:8px 12px!important;border-radius:999px!important;background:#111!important;color:white!important;font:700 14px sans-serif!important;box-shadow:0 4px 18px rgba(0,0,0,.35)!important}';document.documentElement.appendChild(s)}\n"+"function badge(t){var b=document.getElementById('__vcbadge');if(!b){b=document.createElement('div');b.id='__vcbadge';b.className='__vcBadge';document.documentElement.appendChild(b)}b.textContent=t}\n"+"function clr(c){document.querySelectorAll(c).forEach(e=>e.classList.remove(c.slice(1)))}\n";
  static final String JS_LIST="(function(){try{"+COMMON+"sty();function ls(r){var o=[];Array.from(r.querySelectorAll('a[href]')).forEach(a=>{try{var h=new URL(a.getAttribute('href'),location.href).href;if(/^https?:/.test(h))o.push(h)}catch(e){}});return Array.from(new Set(o))}"+"function choose(c){var b=c,bs=-1,cur=c;for(var d=0;d<9&&cur&&cur!==document.body;d++,cur=cur.parentElement){var r=cur.getBoundingClientRect(), l=ls(cur).length, t=C(cur.innerText).length, sc=l*100000+Math.min((r.width||0)*(r.height||0),900000)+Math.min(t,4000);if(l>=2&&sc>bs){b=cur;bs=sc}}return b}"+"function pick(ev){ev.preventDefault();ev.stopPropagation();var p=ev.changedTouches?ev.changedTouches[0]:ev,c=document.elementFromPoint(p.clientX,p.clientY);if(!c){VC.err('선택 실패');return false}var b=choose(c), arr=ls(b);clr('.__vcList');b.classList.add('__vcList');badge('목록 선택됨 · 링크 '+arr.length+'개');VC.list(JSON.stringify({url:location.href,links:arr,preview:C(b.innerText).slice(0,700)}));document.removeEventListener('click',pick,true);document.removeEventListener('touchend',pick,true);return false}"+"document.addEventListener('click',pick,true);document.addEventListener('touchend',pick,true);badge('선택 모드 · 글 목록 전체를 터치');}catch(e){VC.err(String(e&&e.message?e.message:e))}})();";
  static final String JS_BODY="(function(){try{"+COMMON+"sty();function choose(c){var b=c,cur=c;for(var d=0;d<7&&cur&&cur!==document.body;d++,cur=cur.parentElement){var t=C(cur.innerText),r=cur.getBoundingClientRect();if(t.length>=80&&r.height>=60)b=cur;if(t.length>=300&&r.height>=120){b=cur;break}}return b}"+"function pick(ev){ev.preventDefault();ev.stopPropagation();var p=ev.changedTouches?ev.changedTouches[0]:ev,c=document.elementFromPoint(p.clientX,p.clientY);if(!c){VC.err('선택 실패');return false}var b=choose(c), s=css(b), pv=C(b.innerText).slice(0,900);clr('.__vcBody');b.classList.add('__vcBody');badge('본문 선택됨 · 초록색 영역');VC.body(s,pv);document.removeEventListener('click',pick,true);document.removeEventListener('touchend',pick,true);return false}"+"document.addEventListener('click',pick,true);document.addEventListener('touchend',pick,true);badge('선택 모드 · 본문 영역을 터치');}catch(e){VC.err(String(e&&e.message?e.message:e))}})();";
}
