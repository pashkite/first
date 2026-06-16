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
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(10,10,10,10);
    LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
    url=new EditText(this); url.setSingleLine(true); url.setText("https://example.com"); url.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    Button go=B("이동"), retry=B("재시도"), open=B("외부"), help=B("?");
    go.setOnClickListener(v->go()); retry.setOnClickListener(v->retry()); open.setOnClickListener(v->openExternal()); help.setOnClickListener(v->help());
    top.addView(url); top.addView(go); top.addView(retry); top.addView(open); top.addView(help);

    LinearLayout r1=new LinearLayout(this); r1.setOrientation(LinearLayout.HORIZONTAL);
    Button list=B("목록범위 선택"), body=B("본문영역 선택"), start=B("전체수집 시작");
    list.setOnClickListener(v->selList()); body.setOnClickListener(v->selBody()); start.setOnClickListener(v->start());
    r1.addView(list,W()); r1.addView(body,W()); r1.addView(start,W());

    LinearLayout r2=new LinearLayout(this); r2.setOrientation(LinearLayout.HORIZONTAL);
    Button save=B("다운로드 저장"), copy=B("결과 복사"), reset=B("초기화");
    save.setOnClickListener(v->save()); copy.setOnClickListener(v->copy()); reset.setOnClickListener(v->reset());
    r2.addView(save,W()); r2.addView(copy,W()); r2.addView(reset,W());

    stat=new TextView(this); stat.setTextSize(13); stat.setPadding(8,8,8,8); st("v0.7 안정판: v0.4 방식 WebView. 1. 목록범위 선택 → 2. 글 하나 열기 → 3. 본문영역 선택 → 4. 전체수집 시작");

    w=new WebView(this); w.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
    WebSettings s=w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setLoadsImagesAutomatically(true);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    w.addJavascriptInterface(new Bridge(),"VC");
    w.setWebChromeClient(new WebChromeClient());
    w.setWebViewClient(new WebViewClient(){
      public void onPageFinished(WebView v,String u){super.onPageFinished(v,u); url.setText(u); if(running)h.postDelayed(()->grab(),1400);}
      public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err){ if(Build.VERSION.SDK_INT>=23 && req!=null && req.isForMainFrame()) st("페이지 로드 실패: "+err.getDescription()+". v0.4에서도 같은지 확인해줘."); }
      @SuppressWarnings("deprecation") public void onReceivedError(WebView v,int code,String desc,String failingUrl){ st("페이지 로드 실패: "+desc+". v0.4에서도 같은지 확인해줘."); }
      public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req){return false;}
      @SuppressWarnings("deprecation") public boolean shouldOverrideUrlLoading(WebView v,String u){return false;}
    });

    ScrollView sv=new ScrollView(this); sv.setLayoutParams(new LinearLayout.LayoutParams(-1,230));
    out=new TextView(this); out.setTextSize(13); out.setPadding(8,8,8,8); out.setText("결과가 여기에 표시돼."); sv.addView(out);
    root.addView(top); root.addView(r1); root.addView(r2); root.addView(stat); root.addView(w); root.addView(sv);
    setContentView(root); w.loadUrl(url.getText().toString());
  }

  Button B(String t){Button b=new Button(this);b.setText(t);return b;} LinearLayout.LayoutParams W(){return new LinearLayout.LayoutParams(0,-2,1);} void toast(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
  void st(String m){stat.setText(m+"\n상태: 목록 "+links.size()+"개 / 본문 "+(bodySel.length()>0?"선택됨":"미선택"));}
  void go(){String u=url.getText().toString().trim(); if(u.length()==0)return; if(!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u; w.loadUrl(u);} 
  void retry(){String u=w.getUrl(); if(u==null||u.length()==0)u=url.getText().toString(); if(u!=null&&u.length()>0){st("재시도 중: "+u);w.loadUrl(u);}}
  void openExternal(){try{String u=w.getUrl(); if(u==null||u.length()==0)u=url.getText().toString(); if(!u.startsWith("http"))u="https://"+u; startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){toast("외부 브라우저 열기 실패");}}
  void selList(){if(running){toast("수집 중");return;} w.evaluateJavascript(JS_LIST,null); st("노란색으로 기억할 글 목록 전체 범위를 터치해.");}
  void selBody(){if(running){toast("수집 중");return;} w.evaluateJavascript(JS_BODY,null); st("글 내부에서 가져올 본문 영역을 터치해.");}
  void start(){if(links.isEmpty()){toast("목록범위 먼저 선택");return;} if(bodySel.length()==0){toast("본문영역 먼저 선택");return;} blocks.clear(); idx=0; running=true; st("전체수집 시작: 0 / "+links.size()); w.loadUrl(links.get(0));}
  void grab(){if(!running||idx>=links.size())return; String q=JSONObject.quote(bodySel); String js="(function(){function C(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}var e=null;try{e=document.querySelector("+q+")}catch(x){}return JSON.stringify({title:document.title||'',url:location.href,text:e?C(e.innerText||e.textContent):''})})()"; w.evaluateJavascript(js,v->got(v));}
  void got(String v){try{if(v==null)v="{}"; if(v.startsWith("\"")&&v.endsWith("\""))v=new JSONArray("["+v+"]").getString(0); JSONObject o=new JSONObject(v); String title=o.optString("title","글 "+(idx+1)); String text=o.optString("text",""); String u=o.optString("url",links.get(idx)); blocks.add(title+"\n"+u+"\n\n"+(text.length()>0?text:"[본문을 찾지 못했어. 본문영역을 다시 선택해봐.]")+"\n\n------------------------------\n"); idx++; last=join(); out.setText("수집 중: "+idx+" / "+links.size()+"\n\n"+last); if(idx<links.size()){st("수집 중: "+idx+" / "+links.size()); w.loadUrl(links.get(idx));}else{running=false; st("수집 완료. 다운로드 저장을 눌러 txt로 저장해."); toast("수집 완료");}}catch(Exception e){running=false;st("수집 오류: "+e.getMessage());}}
  String join(){StringBuilder sb=new StringBuilder();for(String x:blocks)sb.append(x).append('\n');return sb.toString().trim();}
  void copy(){String t=last.length()>0?last:out.getText().toString(); ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("visual crawler",t)); toast("복사 완료");}
  void save(){String t=last.length()>0?last:out.getText().toString(); if(t.trim().length()==0){toast("저장할 결과 없음");return;} String name="visual_crawler_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date())+".txt"; try{ if(Build.VERSION.SDK_INT>=29){ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"text/plain");cv.put(MediaStore.Downloads.IS_PENDING,1);Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);OutputStream os=getContentResolver().openOutputStream(uri);if(os==null)throw new IOException("stream null");os.write(t.getBytes(StandardCharsets.UTF_8));os.close();cv.clear();cv.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(uri,cv,null,null);}else{File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);d.mkdirs();FileOutputStream f=new FileOutputStream(new File(d,name));f.write(t.getBytes(StandardCharsets.UTF_8));f.close();} st("저장 완료: Download/"+name); toast("저장 완료"); }catch(Exception e){ st("저장 실패: "+e.getMessage()); toast("저장 실패"); }}
  void reset(){ running=false; links.clear(); bodySel=""; listUrl=""; last=""; blocks.clear(); out.setText("초기화 완료."); st("초기화 완료. 목록범위 선택부터 시작해."); }
  void help(){new AlertDialog.Builder(this).setTitle("사용법").setMessage("1. 목록 페이지에서 [목록범위 선택]을 누르고 글 목록 전체를 감싸는 영역을 터치해.\n\n2. 앱이 그 안의 글 링크를 기억해. 글 내부 페이지로 이동해도 기억은 유지돼.\n\n3. 글 하나를 열고 [본문영역 선택]을 눌러 가져올 본문 부분을 터치해.\n\n4. [전체수집 시작]을 누르면 기억한 링크를 하나씩 열어 본문 텍스트만 수집해.\n\n5. [다운로드 저장]을 누르면 Download 폴더에 txt로 저장돼.\n\n이 버전은 v0.4처럼 기본 WebView 설정을 사용해. User-Agent 강제 변경은 제거했어.").setPositiveButton("확인",null).show();}

  public class Bridge{
    @JavascriptInterface public void list(String json){ runOnUiThread(()->{try{JSONObject o=new JSONObject(json); JSONArray a=o.getJSONArray("links"); LinkedHashSet<String> set=new LinkedHashSet<>(); for(int i=0;i<a.length();i++){String x=a.optString(i); if(x.startsWith("http"))set.add(x);} links.clear(); links.addAll(set); listUrl=o.optString("url",w.getUrl()); out.setText("목록범위 선택 완료\n\n기억한 글 링크: "+links.size()+"개\n목록 페이지: "+listUrl+"\n\n선택 영역 미리보기:\n"+o.optString("preview")+"\n\n글 내부로 들어가도 이 목록은 계속 기억돼."); st("목록 "+links.size()+"개 기억됨. 샘플글 열기 또는 직접 글 하나 접속 후 본문영역 선택.");}catch(Exception e){st("목록 처리 오류: "+e.getMessage());}});}
    @JavascriptInterface public void body(String sel,String preview){ runOnUiThread(()->{ bodySel=sel; out.setText("본문영역 선택 완료\n\n본문 미리보기:\n"+preview+"\n\n목록 "+links.size()+"개는 계속 기억 중. 이제 전체수집 시작."); st("본문영역 기억됨. 전체수집 시작 가능."); });}
    @JavascriptInterface public void err(String m){ runOnUiThread(()->st("오류: "+m)); }
  }
  public void onBackPressed(){ if(running){toast("수집 중");return;} if(w.canGoBack())w.goBack(); else super.onBackPressed(); }

  static final String COMMON="function C(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}\n"+"function css(e){if(!e)return'';if(e.id&&!/^\\d+$/.test(e.id))return'#'+CSS.escape(e.id);var p=[];while(e&&e!==document.body){var n=e.tagName.toLowerCase();var cs=Array.from(e.classList||[]).filter(c=>c&&!c.startsWith('__vc')&&!/^\\d+$/.test(c)).slice(0,2);if(cs.length)n+='.'+cs.map(CSS.escape).join('.');var pa=e.parentElement;if(pa){var s=Array.from(pa.children).filter(x=>x.tagName===e.tagName);if(s.length>1)n+=':nth-of-type('+(s.indexOf(e)+1)+')'}p.unshift(n);e=pa}return p.join(' > ')}\n"+"function sty(){if(document.getElementById('__vcsty'))return;var s=document.createElement('style');s.id='__vcsty';s.textContent='.__vcpick{outline:4px solid #ffd000!important;outline-offset:2px!important;background:rgba(255,208,0,.12)!important}';document.documentElement.appendChild(s)}\n"+"function clr(){document.querySelectorAll('.__vcpick').forEach(e=>e.classList.remove('__vcpick'))}\n";
  static final String JS_LIST="(function(){try{"+COMMON+"sty();function ls(r){var o=[];Array.from(r.querySelectorAll('a[href]')).forEach(a=>{try{var h=new URL(a.getAttribute('href'),location.href).href;if(/^https?:/.test(h))o.push(h)}catch(e){}});return Array.from(new Set(o))}"+"function choose(c){var b=c,bs=-1,cur=c;for(var d=0;d<9&&cur&&cur!==document.body;d++,cur=cur.parentElement){var r=cur.getBoundingClientRect(), l=ls(cur).length, t=C(cur.innerText).length, sc=l*100000+Math.min((r.width||0)*(r.height||0),900000)+Math.min(t,4000);if(l>=2&&sc>bs){b=cur;bs=sc}}return b}"+"function pick(ev){ev.preventDefault();ev.stopPropagation();var p=ev.changedTouches?ev.changedTouches[0]:ev,c=document.elementFromPoint(p.clientX,p.clientY);if(!c){VC.err('선택 실패');return false}var b=choose(c);clr();b.classList.add('__vcpick');VC.list(JSON.stringify({url:location.href,links:ls(b),preview:C(b.innerText).slice(0,700)}));document.removeEventListener('click',pick,true);document.removeEventListener('touchend',pick,true);return false}"+"document.addEventListener('click',pick,true);document.addEventListener('touchend',pick,true);}catch(e){VC.err(String(e&&e.message?e.message:e))}})();";
  static final String JS_BODY="(function(){try{"+COMMON+"sty();function choose(c){var b=c,cur=c;for(var d=0;d<7&&cur&&cur!==document.body;d++,cur=cur.parentElement){var t=C(cur.innerText),r=cur.getBoundingClientRect();if(t.length>=80&&r.height>=60)b=cur;if(t.length>=300&&r.height>=120){b=cur;break}}return b}"+"function pick(ev){ev.preventDefault();ev.stopPropagation();var p=ev.changedTouches?ev.changedTouches[0]:ev,c=document.elementFromPoint(p.clientX,p.clientY);if(!c){VC.err('선택 실패');return false}var b=choose(c);clr();b.classList.add('__vcpick');VC.body(css(b),C(b.innerText).slice(0,900));document.removeEventListener('click',pick,true);document.removeEventListener('touchend',pick,true);return false}"+"document.addEventListener('click',pick,true);document.addEventListener('touchend',pick,true);}catch(e){VC.err(String(e&&e.message?e.message:e))}})();";
}
