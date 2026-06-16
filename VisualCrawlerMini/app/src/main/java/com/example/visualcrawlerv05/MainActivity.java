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
  WebView web; EditText urlInput; TextView status, result;
  ArrayList<String> links=new ArrayList<>(), titles=new ArrayList<>(), blocks=new ArrayList<>();
  String listPageUrl="", bodySelector="", bodyNote="", outputText="";
  boolean collecting=false; int currentIndex=0; Handler handler=new Handler(Looper.getMainLooper());

  @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
  public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setFitsSystemWindows(true); root.setPadding(dp(10),dp(44),dp(10),dp(10));
    TextView title=new TextView(this); title.setText("Visual Crawler v0.9 · 링크/이미지 선택판"); title.setTextSize(15); title.setPadding(4,0,4,6);
    LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
    urlInput=new EditText(this); urlInput.setSingleLine(true); urlInput.setHint("https://example.com"); urlInput.setText("https://example.com"); urlInput.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    Button go=btn("이동"), help=btn("?"); go.setOnClickListener(v->openUrl()); help.setOnClickListener(v->showHelp()); top.addView(urlInput); top.addView(go); top.addView(help);
    LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
    Button list=btn("목록범위 선택"), body=btn("본문요소 선택"), start=btn("전체수집 시작");
    list.setOnClickListener(v->enableListSelect()); body.setOnClickListener(v->enableBodySelect()); start.setOnClickListener(v->startCollect()); row1.addView(list,weight()); row1.addView(body,weight()); row1.addView(start,weight());
    LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
    Button save=btn("다운로드 저장"), copy=btn("결과 복사"), reset=btn("초기화");
    save.setOnClickListener(v->saveDownloads()); copy.setOnClickListener(v->copyResult()); reset.setOnClickListener(v->resetState()); row2.addView(save,weight()); row2.addView(copy,weight()); row2.addView(reset,weight());
    status=new TextView(this); status.setTextSize(13); status.setPadding(8,8,8,8); setStatus("v0.9: 링크는 표시 텍스트, 이미지는 설명/주소, 일반 영역은 텍스트 블록으로 선택해.");
    web=new WebView(this); web.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadsImagesAutomatically(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    web.addJavascriptInterface(new Bridge(),"VisualCrawler"); web.setWebChromeClient(new WebChromeClient());
    web.setWebViewClient(new WebViewClient(){
      public void onPageFinished(WebView view,String url){super.onPageFinished(view,url); urlInput.setText(url); if(collecting) handler.postDelayed(()->extractCurrent(),1300);}
      public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err){if(Build.VERSION.SDK_INT>=23 && req!=null && req.isForMainFrame()) setStatus("페이지 로드 실패: "+err.getDescription()+". v0.4 방식 WebView 유지 중.");}
      @SuppressWarnings("deprecation") public void onReceivedError(WebView view,int code,String desc,String failingUrl){setStatus("페이지 로드 실패: "+desc+". v0.4 방식 WebView 유지 중.");}
    });
    ScrollView sv=new ScrollView(this); sv.setLayoutParams(new LinearLayout.LayoutParams(-1,250)); result=new TextView(this); result.setTextSize(13); result.setPadding(8,8,8,8); result.setText("결과가 여기에 표시돼.\n\n노란색=목록, 초록색=본문요소.\n본문에서 링크를 누르면 링크 안의 글자, 이미지를 누르면 이미지 설명/주소가 들어가."); sv.addView(result);
    root.addView(title); root.addView(top); root.addView(row1); root.addView(row2); root.addView(status); root.addView(web); root.addView(sv); setContentView(root); web.loadUrl(urlInput.getText().toString());
  }

  int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);} Button btn(String t){Button b=new Button(this); b.setText(t); b.setTextSize(12); return b;} LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1);} void toast(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
  void setStatus(String m){status.setText(m+"\n상태: 목록 "+links.size()+"개 기억 / 본문 "+(bodySelector.length()>0?"선택됨":"미선택")+(collecting?" / 수집 중 "+currentIndex+"/"+links.size():""));}
  void openUrl(){String u=urlInput.getText().toString().trim(); if(u.length()==0)return; if(!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u; web.loadUrl(u);} 
  String asset(String name){try(InputStream in=getAssets().open(name)){ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; while((n=in.read(buf))>0) out.write(buf,0,n); return out.toString("UTF-8");}catch(Exception e){return "window.VisualCrawler.onError('asset load failed');";}}
  void enableListSelect(){if(collecting){toast("수집 중");return;} web.evaluateJavascript(asset("select_list.js"),null); setStatus("노란색으로 남길 글 목록 전체 영역을 터치해.");}
  void enableBodySelect(){if(collecting){toast("수집 중");return;} web.evaluateJavascript(asset("select_body.js"),null); setStatus("본문요소를 터치해. 링크는 글자, 이미지는 설명/주소로 잡아.");}
  void startCollect(){if(links.isEmpty()){toast("목록범위 먼저 선택");return;} if(bodySelector.length()==0){toast("본문요소 먼저 선택");return;} blocks.clear(); currentIndex=0; collecting=true; outputText=""; String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.KOREA).format(new Date()); blocks.add("Visual Crawler 수집 결과\n생성: "+stamp+"\n목록 페이지: "+listPageUrl+"\n총 링크: "+links.size()+"개\n본문 선택 방식: "+bodyNote+"\n본문 선택자: "+bodySelector+"\n\n==============================\n"); setStatus("전체수집 시작: 0 / "+links.size()); web.loadUrl(links.get(0));}

  void extractCurrent(){
    if(!collecting||currentIndex>=links.size())return;
    String q=JSONObject.quote(bodySelector);
    String js="(function(){function C(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}function imgInfo(img){if(!img)return '';var alt=C(img.getAttribute('alt')||img.getAttribute('title')||'');var cap='';var fig=img.closest&&img.closest('figure');if(fig){var fc=fig.querySelector('figcaption');if(fc)cap=C(fc.innerText||fc.textContent)}var src=img.currentSrc||img.src||img.getAttribute('src')||'';return C([alt,cap,src?('[이미지] '+src):''].filter(Boolean).join('\\n'))||'[이미지: 설명 없음]'}function textOf(e){if(!e)return '';if(e.tagName==='IMG')return imgInfo(e);if(e.tagName==='A'){var t=C(e.innerText||e.textContent);if(t)return t;var im=e.querySelector('img');if(im)return imgInfo(im);return e.href?('[링크] '+e.href):'[링크: 표시 텍스트 없음]'}var t=C(e.innerText||e.textContent);if(t)return t;var im=e.querySelector&&e.querySelector('img');if(im)return imgInfo(im);return ''}var e=null;try{e=document.querySelector("+q+")}catch(x){}return JSON.stringify({title:document.title||'',url:location.href,text:textOf(e)})})()";
    web.evaluateJavascript(js,v->onExtracted(v));
  }
  void onExtracted(String v){try{if(v==null)v="{}"; if(v.startsWith("\"")&&v.endsWith("\""))v=new JSONArray("["+v+"]").getString(0); JSONObject o=new JSONObject(v); String title=o.optString("title",titles.size()>currentIndex?titles.get(currentIndex):"글 "+(currentIndex+1)); String text=o.optString("text",""); String u=o.optString("url",links.get(currentIndex)); String block="["+(currentIndex+1)+"/"+links.size()+"] "+title+"\nURL: "+u+"\n본문 글자수: "+text.length()+"\n\n"+(text.length()>0?text:"[선택한 요소에서 텍스트/이미지 정보를 찾지 못했어.]")+"\n\n------------------------------\n"; blocks.add(block); currentIndex++; outputText=join(); result.setText("수집 중: "+currentIndex+" / "+links.size()+"\n\n"+outputText); if(currentIndex<links.size()){setStatus("수집 중: "+currentIndex+" / "+links.size()); web.loadUrl(links.get(currentIndex));}else{collecting=false; setStatus("수집 완료. 파일 상단에서 선택 방식과 링크 수를 확인 가능."); toast("수집 완료");}}catch(Exception e){collecting=false;setStatus("수집 오류: "+e.getMessage());}}
  String join(){StringBuilder sb=new StringBuilder(); for(String x:blocks)sb.append(x).append('\n'); return sb.toString().trim();}
  void copyResult(){String t=outputText.length()>0?outputText:result.getText().toString(); ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("visual crawler",t)); toast("복사 완료");}
  void saveDownloads(){String t=outputText.length()>0?outputText:result.getText().toString(); if(t.trim().length()==0){toast("저장할 결과 없음");return;} String name="visual_crawler_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date())+".txt"; try{ if(Build.VERSION.SDK_INT>=29){ContentValues cv=new ContentValues(); cv.put(MediaStore.Downloads.DISPLAY_NAME,name); cv.put(MediaStore.Downloads.MIME_TYPE,"text/plain"); cv.put(MediaStore.Downloads.IS_PENDING,1); Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv); OutputStream os=getContentResolver().openOutputStream(uri); os.write(t.getBytes(StandardCharsets.UTF_8)); os.close(); cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING,0); getContentResolver().update(uri,cv,null,null);} else {File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); d.mkdirs(); FileOutputStream f=new FileOutputStream(new File(d,name)); f.write(t.getBytes(StandardCharsets.UTF_8)); f.close();} setStatus("저장 완료: Download/"+name); toast("저장 완료");}catch(Exception e){setStatus("저장 실패: "+e.getMessage()); toast("저장 실패");}}
  void resetState(){collecting=false; links.clear(); titles.clear(); bodySelector=""; bodyNote=""; listPageUrl=""; outputText=""; blocks.clear(); result.setText("초기화 완료. 목록범위 선택부터 시작해."); setStatus("초기화 완료.");}
  void showHelp(){new AlertDialog.Builder(this).setTitle("사용법").setMessage("v0.9\n\n목록범위 선택은 글 목록 전체를 잡고 링크를 기억해.\n본문요소 선택은 누른 대상에 따라 다르게 동작해.\n\n· 링크 터치: 링크 안의 보이는 텍스트 저장\n· 이미지 터치: 이미지 alt/title/캡션/주소 저장\n· 일반 텍스트 터치: 가까운 텍스트 블록 저장\n\n결과는 Download 폴더에 txt로 저장돼.").setPositiveButton("확인",null).show();}

  public class Bridge{
    @JavascriptInterface public void onInfo(String msg){runOnUiThread(()->setStatus(msg));}
    @JavascriptInterface public void onError(String msg){runOnUiThread(()->setStatus("오류: "+msg));}
    @JavascriptInterface public void onListSelected(String raw){runOnUiThread(()->{try{JSONObject obj=new JSONObject(raw); JSONArray arr=obj.getJSONArray("links"); links.clear(); titles.clear(); LinkedHashSet<String> seen=new LinkedHashSet<>(); for(int i=0;i<arr.length();i++){JSONObject it=arr.getJSONObject(i); String u=it.optString("url",""); if(u.startsWith("http")&&!seen.contains(u)){seen.add(u); links.add(u); titles.add(it.optString("text",""));}} listPageUrl=obj.optString("pageUrl",web.getUrl()); result.setText("목록범위 선택 완료\n\n기억한 글 링크: "+links.size()+"개\n목록 페이지: "+listPageUrl+"\n\n선택 영역 미리보기:\n"+obj.optString("preview")+"\n\n노란색 테두리가 선택한 목록이야."); setStatus("목록 "+links.size()+"개 기억됨. 글 하나를 열고 본문요소 선택을 눌러.");}catch(Exception e){setStatus("목록 처리 오류: "+e.getMessage());}});}
    @JavascriptInterface public void onBodySelected(String selector,String preview,String note){runOnUiThread(()->{bodySelector=selector; bodyNote=note; result.setText("본문요소 선택 완료\n\n선택 방식: "+note+"\n본문 선택자:\n"+selector+"\n\n미리보기:\n"+preview+"\n\n초록색 테두리가 선택한 요소야."); setStatus("본문요소 기억됨: "+note);});}
  }
  public void onBackPressed(){if(collecting){toast("수집 중");return;} if(web.canGoBack())web.goBack(); else super.onBackPressed();}
}
