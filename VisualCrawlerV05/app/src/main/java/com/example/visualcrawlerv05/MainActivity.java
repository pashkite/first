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
  boolean running=false; int idx=0; Handler h=new Handler();

  @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
  public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(10,10,10,10);
    LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
    url=new EditText(this); url.setSingleLine(true); url.setText("https://example.com"); url.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    Button go=btn("이동",v->loadUrl());
    Button help=btn("?",v->help());
    top.addView(url); top.addView(go); top.addView(help);
    stat=new TextView(this); stat.setText("목록 페이지에서 [목록범위 선택]부터 시작해."); stat.setTextSize(13); stat.setPadding(6,6,6,6);
    HorizontalScrollView hsv=new HorizontalScrollView(this); LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL);
    bar.addView(btn("목록범위 선택",v->selectList()));
    bar.addView(btn("샘플글 열기",v->openSample()));
    bar.addView(btn("본문영역 선택",v->selectBody()));
    bar.addView(btn("전체수집 시작",v->start()));
    bar.addView(btn("다운로드 저장",v->save()));
    bar.addView(btn("초기화",v->reset()));
    hsv.addView(bar);
    w=new WebView(this); w.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
    WebSettings s=w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadsImagesAutomatically(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    w.addJavascriptInterface(new Bridge(),"VC");
    w.setWebChromeClient(new WebChromeClient());
    w.setWebViewClient(new WebViewClient(){ public void onPageFinished(WebView v,String u){ url.setText(u); if(running)h.postDelayed(()->extract(),900); }});
    ScrollView sv=new ScrollView(this); sv.setLayoutParams(new LinearLayout.LayoutParams(-1,260));
    out=new TextView(this); out.setText("결과가 여기에 표시돼.\n\n목록범위 선택 → 샘플글 열기 → 본문영역 선택 → 전체수집 시작 → 다운로드 저장"); out.setTextSize(13); out.setPadding(8,8,8,8); sv.addView(out);
    root.addView(top); root.addView(stat); root.addView(hsv); root.addView(w); root.addView(sv);
    setContentView(root); w.loadUrl(url.getText().toString());
  }

  Button btn(String t, View.OnClickListener l){ Button b=new Button(this); b.setText(t); b.setOnClickListener(l); return b; }
  void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
  void st(String s){ stat.setText(s); }
  void loadUrl(){ String r=url.getText().toString().trim(); if(r.length()==0)return; w.loadUrl(r.startsWith("http")?r:"https://"+r); }

  void help(){
    new AlertDialog.Builder(this).setTitle("사용법")
      .setMessage("1. 글 목록 페이지에서 [목록범위 선택]\n2. 글 목록 전체를 감싸는 영역을 터치\n   → 앱이 그 안의 글 링크를 기억함\n3. [샘플글 열기] 또는 직접 글 하나를 눌러 내부 페이지 이동\n4. [본문영역 선택]으로 가져올 본문 영역 터치\n5. [전체수집 시작]\n   → 처음 기억한 링크들을 하나씩 열고 같은 본문 영역만 수집\n6. [다운로드 저장]\n   → Downloads 폴더에 txt 저장\n\n페이지를 이동해도 목록 선택 정보는 앱 내부에 계속 기억돼.")
      .setPositiveButton("확인",null).show();
  }

  void selectList(){ if(running){toast("수집 중");return;} w.evaluateJavascript(JS_LIST,null); st("글 목록 전체를 감싸는 영역을 터치해."); }
  void selectBody(){ if(links.isEmpty()){toast("목록 먼저 선택");return;} w.evaluateJavascript(JS_BODY,null); st("글 내부 페이지에서 가져올 본문 영역을 터치해. 목록 "+links.size()+"개 기억 중."); }
  void openSample(){ if(links.isEmpty()){toast("목록 먼저 선택");return;} w.loadUrl(links.get(0)); st("샘플글을 열었어. 목록 "+links.size()+"개는 기억 중. 이제 본문영역 선택."); }

  void start(){
    if(links.isEmpty()){toast("목록 먼저 선택");return;}
    if(bodySel.trim().isEmpty()){toast("본문 먼저 선택");return;}
    running=true; idx=0; blocks.clear(); last=""; out.setText("수집 시작...\n"); st("수집 1 / "+links.size()); w.loadUrl(links.get(0));
  }

  void extract(){
    String js="(function(){function c(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}var e=null;try{e=document.querySelector("+JSONObject.quote(bodySel)+")}catch(x){}return JSON.stringify({title:c(document.title),url:location.href,text:e?c(e.innerText||e.textContent):'',ok:!!e})})();";
    w.evaluateJavascript(js,v->{
      try{
        JSONObject o=new JSONObject(dec(v));
        String text=o.optString("text").trim();
        if(text.isEmpty()) text=o.optBoolean("ok")?"(텍스트 없음)":"(본문 영역을 찾지 못함)";
        blocks.add("제목: "+o.optString("title")+"\n주소: "+o.optString("url")+"\n\n"+text+"\n\n------------------------------\n");
        out.setText(join(false));
        idx++; st("수집 "+idx+" / "+links.size());
        if(idx<links.size()) h.postDelayed(()->w.loadUrl(links.get(idx)),700);
        else{ running=false; last=join(true); out.setText(last); st("수집 완료. 다운로드 저장을 누르면 Downloads 폴더에 저장돼."); toast("완료"); }
      }catch(Exception e){ running=false; st("수집 오류: "+e.getMessage()); }
    });
  }

  String dec(String v)throws Exception{ if(v==null||v.equals("null"))return""; return new JSONArray("["+v+"]").getString(0); }
  String join(boolean head){ StringBuilder sb=new StringBuilder(); if(head)sb.append("Visual Crawler v0.5 수집 결과\n목록 페이지: ").append(listUrl).append("\n수집 개수: ").append(blocks.size()).append("\n\n"); for(String b:blocks)sb.append(b).append("\n"); return sb.toString(); }

  void save(){
    String c=(last!=null&&!last.trim().isEmpty())?last:out.getText().toString();
    if(c.trim().isEmpty()){toast("저장할 내용 없음");return;}
    try{
      String name="visual_crawler_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date())+".txt";
      if(Build.VERSION.SDK_INT>=29){
        ContentValues v=new ContentValues(); v.put(MediaStore.MediaColumns.DISPLAY_NAME,name); v.put(MediaStore.MediaColumns.MIME_TYPE,"text/plain"); v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);
        Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
        OutputStream os=getContentResolver().openOutputStream(u); os.write(c.getBytes(StandardCharsets.UTF_8)); os.close();
      }else{
        File f=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),name);
        FileOutputStream os=new FileOutputStream(f); os.write(c.getBytes(StandardCharsets.UTF_8)); os.close();
      }
      st("Downloads 저장 완료: "+name); toast("저장 완료");
    }catch(Exception e){ st("저장 실패: "+e.getMessage()); toast("저장 실패"); }
  }

  void reset(){ running=false; links.clear(); bodySel=""; listUrl=""; last=""; blocks.clear(); out.setText("초기화 완료."); st("초기화 완료. 목록범위 선택부터 시작해."); }

  public class Bridge{
    @JavascriptInterface public void list(String json){ runOnUiThread(()->{
      try{
        JSONObject o=new JSONObject(json); JSONArray a=o.getJSONArray("links"); LinkedHashSet<String> set=new LinkedHashSet<>();
        for(int i=0;i<a.length();i++){String x=a.optString(i); if(x.startsWith("http"))set.add(x);}
        links.clear(); links.addAll(set); listUrl=o.optString("url",w.getUrl());
        out.setText("목록범위 선택 완료\n\n기억한 글 링크: "+links.size()+"개\n목록 페이지: "+listUrl+"\n\n선택 영역 미리보기:\n"+o.optString("preview")+"\n\n글 내부로 들어가도 이 목록은 계속 기억돼.");
        st("목록 "+links.size()+"개 기억됨. 샘플글 열기 또는 직접 글 하나 접속 후 본문영역 선택.");
      }catch(Exception e){st("목록 처리 오류: "+e.getMessage());}
    });}
    @JavascriptInterface public void body(String sel,String preview){ runOnUiThread(()->{ bodySel=sel; out.setText("본문영역 선택 완료\n\n본문 미리보기:\n"+preview+"\n\n목록 "+links.size()+"개는 계속 기억 중. 이제 전체수집 시작."); st("본문영역 기억됨. 전체수집 시작 가능."); });}
    @JavascriptInterface public void err(String m){ runOnUiThread(()->st("오류: "+m)); }
  }

  public void onBackPressed(){ if(running){toast("수집 중");return;} if(w.canGoBack())w.goBack(); else super.onBackPressed(); }

  static final String COMMON=
    "function C(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}\n"+
    "function css(e){if(!e)return'';if(e.id&&!/^\\d+$/.test(e.id))return'#'+CSS.escape(e.id);var p=[];while(e&&e!==document.body){var n=e.tagName.toLowerCase();var cs=Array.from(e.classList||[]).filter(c=>c&&!c.startsWith('__vc')&&!/^\\d+$/.test(c)).slice(0,2);if(cs.length)n+='.'+cs.map(CSS.escape).join('.');var pa=e.parentElement;if(pa){var s=Array.from(pa.children).filter(x=>x.tagName===e.tagName);if(s.length>1)n+=':nth-of-type('+(s.indexOf(e)+1)+')'}p.unshift(n);e=pa}return p.join(' > ')}\n"+
    "function sty(){if(document.getElementById('__vcsty'))return;var s=document.createElement('style');s.id='__vcsty';s.textContent='.__vcpick{outline:4px solid #ffd000!important;outline-offset:2px!important;background:rgba(255,208,0,.12)!important}';document.documentElement.appendChild(s)}\n"+
    "function clr(){document.querySelectorAll('.__vcpick').forEach(e=>e.classList.remove('__vcpick'))}\n";

  static final String JS_LIST="(function(){try{"+COMMON+
    "sty();function ls(r){var o=[];Array.from(r.querySelectorAll('a[href]')).forEach(a=>{try{var h=new URL(a.getAttribute('href'),location.href).href;if(/^https?:/.test(h))o.push(h)}catch(e){}});return Array.from(new Set(o))}"+
    "function choose(c){var b=c,bs=-1,cur=c;for(var d=0;d<9&&cur&&cur!==document.body;d++,cur=cur.parentElement){var r=cur.getBoundingClientRect(), l=ls(cur).length, t=C(cur.innerText).length, sc=l*100000+Math.min((r.width||0)*(r.height||0),900000)+Math.min(t,4000);if(l>=2&&sc>bs){b=cur;bs=sc}}return b}"+
    "function pick(ev){ev.preventDefault();ev.stopPropagation();var p=ev.changedTouches?ev.changedTouches[0]:ev,c=document.elementFromPoint(p.clientX,p.clientY);if(!c){VC.err('선택 실패');return false}var b=choose(c);clr();b.classList.add('__vcpick');VC.list(JSON.stringify({url:location.href,links:ls(b),preview:C(b.innerText).slice(0,700)}));document.removeEventListener('click',pick,true);document.removeEventListener('touchend',pick,true);return false}"+
    "document.addEventListener('click',pick,true);document.addEventListener('touchend',pick,true);}catch(e){VC.err(String(e&&e.message?e.message:e))}})();";

  static final String JS_BODY="(function(){try{"+COMMON+
    "sty();function choose(c){var b=c,cur=c;for(var d=0;d<7&&cur&&cur!==document.body;d++,cur=cur.parentElement){var t=C(cur.innerText),r=cur.getBoundingClientRect();if(t.length>=80&&r.height>=60)b=cur;if(t.length>=300&&r.height>=120){b=cur;break}}return b}"+
    "function pick(ev){ev.preventDefault();ev.stopPropagation();var p=ev.changedTouches?ev.changedTouches[0]:ev,c=document.elementFromPoint(p.clientX,p.clientY);if(!c){VC.err('선택 실패');return false}var b=choose(c);clr();b.classList.add('__vcpick');VC.body(css(b),C(b.innerText).slice(0,900));document.removeEventListener('click',pick,true);document.removeEventListener('touchend',pick,true);return false}"+
    "document.addEventListener('click',pick,true);document.addEventListener('touchend',pick,true);}catch(e){VC.err(String(e&&e.message?e.message:e))}})();";
}
