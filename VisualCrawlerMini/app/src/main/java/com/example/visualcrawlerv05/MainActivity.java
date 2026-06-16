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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
  WebView web; EditText urlInput; TextView status, result;
  ArrayList<String> links=new ArrayList<>(), titles=new ArrayList<>(), htmlBlocks=new ArrayList<>();
  LinkedHashSet<String> imageUrls=new LinkedHashSet<>();
  String listPageUrl="", bodySelector="", bodyNote="", outputText="";
  boolean collecting=false; int currentIndex=0; Handler handler=new Handler(Looper.getMainLooper());

  @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
  public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setFitsSystemWindows(true); root.setPadding(dp(10),dp(44),dp(10),dp(10));
    TextView title=new TextView(this); title.setText("Visual Crawler v0.10 · ZIP 이미지 포함판"); title.setTextSize(15); title.setPadding(4,0,4,6);
    LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
    urlInput=new EditText(this); urlInput.setSingleLine(true); urlInput.setHint("https://example.com"); urlInput.setText("https://example.com"); urlInput.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    Button go=btn("이동"), help=btn("?"); go.setOnClickListener(v->openUrl()); help.setOnClickListener(v->showHelp()); top.addView(urlInput); top.addView(go); top.addView(help);
    LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
    Button list=btn("목록범위 선택"), body=btn("본문요소 선택"), start=btn("전체수집 시작");
    list.setOnClickListener(v->enableListSelect()); body.setOnClickListener(v->enableBodySelect()); start.setOnClickListener(v->startCollect()); row1.addView(list,weight()); row1.addView(body,weight()); row1.addView(start,weight());
    LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
    Button save=btn("ZIP 저장"), copy=btn("HTML 복사"), reset=btn("초기화");
    save.setOnClickListener(v->saveZip()); copy.setOnClickListener(v->copyResult()); reset.setOnClickListener(v->resetState()); row2.addView(save,weight()); row2.addView(copy,weight()); row2.addView(reset,weight());
    status=new TextView(this); status.setTextSize(13); status.setPadding(8,8,8,8); setStatus("v0.10: 결과를 ZIP으로 저장해. ZIP 안에 result.html + images/ 가 들어가.");
    web=new WebView(this); web.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadsImagesAutomatically(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    web.addJavascriptInterface(new Bridge(),"VisualCrawler"); web.setWebChromeClient(new WebChromeClient());
    web.setWebViewClient(new WebViewClient(){
      public void onPageFinished(WebView view,String url){super.onPageFinished(view,url); urlInput.setText(url); if(collecting) handler.postDelayed(()->extractCurrent(),1300);}
      public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err){if(Build.VERSION.SDK_INT>=23 && req!=null && req.isForMainFrame()) setStatus("페이지 로드 실패: "+err.getDescription()+". v0.4 방식 WebView 유지 중.");}
      @SuppressWarnings("deprecation") public void onReceivedError(WebView view,int code,String desc,String failingUrl){setStatus("페이지 로드 실패: "+desc+". v0.4 방식 WebView 유지 중.");}
    });
    ScrollView sv=new ScrollView(this); sv.setLayoutParams(new LinearLayout.LayoutParams(-1,250)); result=new TextView(this); result.setTextSize(13); result.setPadding(8,8,8,8); result.setText("결과가 여기에 표시돼.\n\n노란색=목록, 초록색=본문요소.\nZIP 저장을 누르면 result.html과 images/가 같이 저장돼."); sv.addView(result);
    root.addView(title); root.addView(top); root.addView(row1); root.addView(row2); root.addView(status); root.addView(web); root.addView(sv); setContentView(root); web.loadUrl(urlInput.getText().toString());
  }

  int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);} Button btn(String t){Button b=new Button(this); b.setText(t); b.setTextSize(12); return b;} LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1);} void toast(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
  void setStatus(String m){status.setText(m+"\n상태: 목록 "+links.size()+"개 기억 / 본문 "+(bodySelector.length()>0?"선택됨":"미선택")+(collecting?" / 수집 중 "+currentIndex+"/"+links.size():""));}
  void openUrl(){String u=urlInput.getText().toString().trim(); if(u.length()==0)return; if(!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u; web.loadUrl(u);} 
  String asset(String name){try(InputStream in=getAssets().open(name)){ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; while((n=in.read(buf))>0) out.write(buf,0,n); return out.toString("UTF-8");}catch(Exception e){return "window.VisualCrawler.onError('asset load failed');";}}
  void enableListSelect(){if(collecting){toast("수집 중");return;} web.evaluateJavascript(asset("select_list.js"),null); setStatus("노란색으로 남길 글 목록 전체 영역을 터치해.");}
  void enableBodySelect(){if(collecting){toast("수집 중");return;} web.evaluateJavascript(asset("select_body.js"),null); setStatus("본문요소를 터치해. 링크 텍스트/이미지/텍스트 블록을 HTML로 저장해.");}
  void startCollect(){if(links.isEmpty()){toast("목록범위 먼저 선택");return;} if(bodySelector.length()==0){toast("본문요소 먼저 선택");return;} htmlBlocks.clear(); imageUrls.clear(); currentIndex=0; collecting=true; outputText=""; setStatus("전체수집 시작: 0 / "+links.size()); web.loadUrl(links.get(0));}

  void extractCurrent(){
    if(!collecting||currentIndex>=links.size())return;
    String q=JSONObject.quote(bodySelector);
    String js="(function(){function C(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim()}function abs(u){try{return new URL(u,location.href).href}catch(e){return u||''}}function cleanClone(el){var c=el.cloneNode(true);c.querySelectorAll&&c.querySelectorAll('script,style,noscript,iframe,canvas,svg').forEach(function(x){x.remove()});c.querySelectorAll&&c.querySelectorAll('a').forEach(function(a){var img=a.querySelector&&a.querySelector('img');if(img&&!(C(a.innerText||a.textContent))){var im=img.cloneNode(true);a.replaceWith(im)}else{var sp=document.createElement('span');sp.textContent=C(a.innerText||a.textContent)||'[링크 텍스트 없음]';a.replaceWith(sp)}});c.querySelectorAll&&c.querySelectorAll('img').forEach(function(im){var src=abs(im.currentSrc||im.src||im.getAttribute('src')||'');if(src)im.setAttribute('src',src);im.removeAttribute('srcset');im.removeAttribute('sizes');im.setAttribute('style','max-width:100%;height:auto;display:block;margin:12px 0;')});return c.outerHTML||C(c.innerText||c.textContent)}function images(el){var arr=[];if(!el)return arr;var imgs=[];if(el.tagName==='IMG')imgs=[el];else imgs=Array.from(el.querySelectorAll?el.querySelectorAll('img'):[]);imgs.forEach(function(im){var src=abs(im.currentSrc||im.src||im.getAttribute('src')||'');if(src&&/^https?:/.test(src)&&arr.indexOf(src)<0)arr.push(src)});return arr}function choose(e){if(!e)return null;if(e.tagName==='IMG')return e;if(e.tagName==='A'){var t=C(e.innerText||e.textContent);if(t)return e;var im=e.querySelector&&e.querySelector('img');if(im)return im;return e}return e}var e=null;try{e=document.querySelector("+q+")}catch(x){}e=choose(e);var text=C(e?(e.innerText||e.textContent):'');var html=e?cleanClone(e):'';return JSON.stringify({title:document.title||'',url:location.href,text:text,html:html,images:images(e)})})()";
    web.evaluateJavascript(js,v->onExtracted(v));
  }

  void onExtracted(String v){try{if(v==null)v="{}"; if(v.startsWith("\"")&&v.endsWith("\""))v=new JSONArray("["+v+"]").getString(0); JSONObject o=new JSONObject(v); String title=o.optString("title",titles.size()>currentIndex?titles.get(currentIndex):"글 "+(currentIndex+1)); String text=o.optString("text",""); String u=o.optString("url",links.get(currentIndex)); String html=o.optString("html",escape(text)); JSONArray imgs=o.optJSONArray("images"); if(imgs!=null){for(int i=0;i<imgs.length();i++){String src=imgs.optString(i); if(src.startsWith("http"))imageUrls.add(src);}} String block="<section class='post'><h2>"+escape(title)+"</h2><p class='meta'>["+(currentIndex+1)+"/"+links.size()+"] "+escape(u)+"</p><div class='content'>"+html+"</div></section>"; htmlBlocks.add(block); currentIndex++; outputText=previewHtml(); result.setText("수집 중: "+currentIndex+" / "+links.size()+"\n이미지 후보: "+imageUrls.size()+"개\n\n"+strip(outputText)); if(currentIndex<links.size()){setStatus("수집 중: "+currentIndex+" / "+links.size()); web.loadUrl(links.get(currentIndex));}else{collecting=false; setStatus("수집 완료. ZIP 저장을 누르면 HTML과 이미지를 함께 저장해."); toast("수집 완료");}}catch(Exception e){collecting=false;setStatus("수집 오류: "+e.getMessage());}}

  String previewHtml(){StringBuilder sb=new StringBuilder(); for(String x:htmlBlocks)sb.append(x).append('\n'); return sb.toString();}
  String buildHtml(Map<String,String> map){String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.KOREA).format(new Date()); StringBuilder sb=new StringBuilder(); sb.append("<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Visual Crawler</title><style>body{font-family:sans-serif;line-height:1.65;padding:18px;max-width:860px;margin:auto}img{max-width:100%;height:auto}.post{border-bottom:1px solid #ddd;padding:20px 0}.meta{color:#666;font-size:13px;word-break:break-all}.cover{background:#f6f6f6;border-radius:12px;padding:14px}</style></head><body>"); sb.append("<div class='cover'><h1>Visual Crawler 수집 결과</h1><p>생성: "+escape(stamp)+"<br>목록 페이지: "+escape(listPageUrl)+"<br>총 링크: "+links.size()+"개<br>본문 선택 방식: "+escape(bodyNote)+"<br>이미지 파일: "+map.size()+"개</p></div>"); for(String b:htmlBlocks){String h=b; for(String src:map.keySet()){h=h.replace(src,map.get(src)); h=h.replace(src.replace("&","&amp;"),map.get(src));} sb.append(h);} sb.append("</body></html>"); return sb.toString();}
  String strip(String h){return h.replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim();}
  String escape(String s){if(s==null)return ""; return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
  String extFrom(String url,String type){String e="jpg"; if(type!=null){if(type.contains("png"))e="png"; else if(type.contains("webp"))e="webp"; else if(type.contains("gif"))e="gif"; else if(type.contains("jpeg")||type.contains("jpg"))e="jpg";} else {String lower=url.toLowerCase(Locale.ROOT); if(lower.contains(".png"))e="png"; else if(lower.contains(".webp"))e="webp"; else if(lower.contains(".gif"))e="gif";} return e;}

  void saveZip(){if(htmlBlocks.isEmpty()){toast("저장할 결과 없음");return;} setStatus("ZIP 생성 중... 이미지 다운로드를 시도해."); new Thread(()->{try{LinkedHashMap<String,String> map=new LinkedHashMap<>(); LinkedHashMap<String,byte[]> files=new LinkedHashMap<>(); int n=1; for(String src:imageUrls){try{HttpURLConnection c=(HttpURLConnection)new URL(src).openConnection(); c.setConnectTimeout(12000); c.setReadTimeout(18000); c.setRequestProperty("User-Agent", web.getSettings().getUserAgentString()); String ck=CookieManager.getInstance().getCookie(src); if(ck!=null)c.setRequestProperty("Cookie",ck); c.connect(); String type=c.getContentType(); ByteArrayOutputStream bos=new ByteArrayOutputStream(); InputStream in=c.getInputStream(); byte[] buf=new byte[8192]; int r; while((r=in.read(buf))>0)bos.write(buf,0,r); in.close(); String path="images/img_"+String.format(Locale.ROOT,"%03d",n++)+"."+extFrom(src,type); map.put(src,path); files.put(path,bos.toByteArray());}catch(Exception ex){}}
        String html=buildHtml(map); String name="visual_crawler_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date())+".zip"; if(Build.VERSION.SDK_INT>=29){ContentValues cv=new ContentValues(); cv.put(MediaStore.Downloads.DISPLAY_NAME,name); cv.put(MediaStore.Downloads.MIME_TYPE,"application/zip"); cv.put(MediaStore.Downloads.IS_PENDING,1); Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv); OutputStream os=getContentResolver().openOutputStream(uri); writeZip(os,html,files,map); os.close(); cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING,0); getContentResolver().update(uri,cv,null,null);} else {File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); d.mkdirs(); FileOutputStream fos=new FileOutputStream(new File(d,name)); writeZip(fos,html,files,map); fos.close();}
        runOnUiThread(()->{setStatus("ZIP 저장 완료: Download/"+name+" · 이미지 "+files.size()+"개 포함"); toast("ZIP 저장 완료");});}catch(Exception e){runOnUiThread(()->{setStatus("ZIP 저장 실패: "+e.getMessage()); toast("ZIP 저장 실패");});}}).start();}
  void writeZip(OutputStream os,String html,Map<String,byte[]> imgs,Map<String,String> map)throws Exception{ZipOutputStream z=new ZipOutputStream(os); z.putNextEntry(new ZipEntry("result.html")); z.write(html.getBytes(StandardCharsets.UTF_8)); z.closeEntry(); for(String p:imgs.keySet()){z.putNextEntry(new ZipEntry(p)); z.write(imgs.get(p)); z.closeEntry();} StringBuilder info=new StringBuilder(); info.append("포함 이미지: ").append(imgs.size()).append("개\n"); for(String src:map.keySet())info.append(map.get(src)).append(" <- ").append(src).append("\n"); z.putNextEntry(new ZipEntry("README.txt")); z.write(info.toString().getBytes(StandardCharsets.UTF_8)); z.closeEntry(); z.finish(); z.close();}

  void copyResult(){String t=buildHtml(new LinkedHashMap<>()); ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("visual crawler html",t)); toast("HTML 복사 완료");}
  void resetState(){collecting=false; links.clear(); titles.clear(); bodySelector=""; bodyNote=""; listPageUrl=""; outputText=""; htmlBlocks.clear(); imageUrls.clear(); result.setText("초기화 완료. 목록범위 선택부터 시작해."); setStatus("초기화 완료.");}
  void showHelp(){new AlertDialog.Builder(this).setTitle("사용법").setMessage("v0.10 ZIP 이미지 포함판\n\n1. 목록범위 선택으로 글 목록 전체를 잡아.\n2. 글 하나에서 본문요소를 선택해.\n3. 전체수집 시작을 누르면 같은 요소를 각 글에서 가져와.\n4. ZIP 저장을 누르면 Download 폴더에 zip이 저장돼.\n\nZIP 안에는 result.html과 images/ 폴더가 들어가. 이미지는 가능한 경우 실제 파일로 저장되고, 실패한 이미지는 원격 주소가 남을 수 있어.").setPositiveButton("확인",null).show();}

  public class Bridge{
    @JavascriptInterface public void onInfo(String msg){runOnUiThread(()->setStatus(msg));}
    @JavascriptInterface public void onError(String msg){runOnUiThread(()->setStatus("오류: "+msg));}
    @JavascriptInterface public void onListSelected(String raw){runOnUiThread(()->{try{JSONObject obj=new JSONObject(raw); JSONArray arr=obj.getJSONArray("links"); links.clear(); titles.clear(); LinkedHashSet<String> seen=new LinkedHashSet<>(); for(int i=0;i<arr.length();i++){JSONObject it=arr.getJSONObject(i); String u=it.optString("url",""); if(u.startsWith("http")&&!seen.contains(u)){seen.add(u); links.add(u); titles.add(it.optString("text",""));}} listPageUrl=obj.optString("pageUrl",web.getUrl()); result.setText("목록범위 선택 완료\n\n기억한 글 링크: "+links.size()+"개\n목록 페이지: "+listPageUrl+"\n\n선택 영역 미리보기:\n"+obj.optString("preview")+"\n\n노란색 테두리가 선택한 목록이야."); setStatus("목록 "+links.size()+"개 기억됨. 글 하나를 열고 본문요소 선택을 눌러.");}catch(Exception e){setStatus("목록 처리 오류: "+e.getMessage());}});}
    @JavascriptInterface public void onBodySelected(String selector,String preview,String note){runOnUiThread(()->{bodySelector=selector; bodyNote=note; result.setText("본문요소 선택 완료\n\n선택 방식: "+note+"\n본문 선택자:\n"+selector+"\n\n미리보기:\n"+preview+"\n\n초록색 테두리가 선택한 요소야. ZIP 저장 시 이미지는 파일로 내려받아 HTML에 넣어."); setStatus("본문요소 기억됨: "+note);});}
  }
  public void onBackPressed(){if(collecting){toast("수집 중");return;} if(web.canGoBack())web.goBack(); else super.onBackPressed();}
}
