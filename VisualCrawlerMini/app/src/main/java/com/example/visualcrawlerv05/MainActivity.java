package com.example.visualcrawlerv05;

import android.annotation.SuppressLint;
import android.app.*;
import android.os.*;
import android.provider.MediaStore;
import android.content.*;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private EditText urlInput;
    private TextView status, result;
    private final ArrayList<String> links = new ArrayList<>();
    private final ArrayList<String> linkTitles = new ArrayList<>();
    private String listPageUrl = "";
    private String bodySelector = "";
    private String outputText = "";
    private final StringBuilder collected = new StringBuilder();
    private boolean collecting = false;
    private int currentIndex = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8,8,8,8);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://example.com");
        urlInput.setText("https://example.com");
        urlInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button go = btn("이동");
        Button help = btn("?");
        go.setOnClickListener(v -> openUrl());
        help.setOnClickListener(v -> showHelp());
        top.addView(urlInput); top.addView(go); top.addView(help);

        LinearLayout tools1 = new LinearLayout(this);
        tools1.setOrientation(LinearLayout.HORIZONTAL);
        Button listBtn = btn("목록영역 선택");
        Button bodyBtn = btn("본문영역 선택");
        Button startBtn = btn("전체수집 시작");
        listBtn.setOnClickListener(v -> enableListSelect());
        bodyBtn.setOnClickListener(v -> enableBodySelect());
        startBtn.setOnClickListener(v -> startCollect());
        tools1.addView(listBtn, weight()); tools1.addView(bodyBtn, weight()); tools1.addView(startBtn, weight());

        LinearLayout tools2 = new LinearLayout(this);
        tools2.setOrientation(LinearLayout.HORIZONTAL);
        Button saveBtn = btn("다운로드 저장");
        Button copyBtn = btn("결과 복사");
        Button resetBtn = btn("초기화");
        saveBtn.setOnClickListener(v -> saveDownloads());
        copyBtn.setOnClickListener(v -> copyResult());
        resetBtn.setOnClickListener(v -> resetState());
        tools2.addView(saveBtn, weight()); tools2.addView(copyBtn, weight()); tools2.addView(resetBtn, weight());

        status = new TextView(this);
        status.setTextSize(13f);
        status.setPadding(8,8,8,8);
        status.setText("1. 목록 페이지에서 목록영역 선택 → 2. 글 하나 열기 → 3. 본문영역 선택 → 4. 전체수집 시작");

        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        web.addJavascriptInterface(new Bridge(), "VisualCrawler");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (collecting) handler.postDelayed(() -> extractCurrentBody(), 1200);
            }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 230));
        result = new TextView(this);
        result.setTextSize(13f);
        result.setPadding(8,8,8,8);
        result.setText("결과가 여기에 표시돼.");
        scroll.addView(result);

        root.addView(top); root.addView(tools1); root.addView(tools2); root.addView(status); root.addView(web); root.addView(scroll);
        setContentView(root);
        web.loadUrl(urlInput.getText().toString());
    }

    private Button btn(String t){ Button b=new Button(this); b.setText(t); return b; }
    private LinearLayout.LayoutParams weight(){ return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); }
    private void toast(String m){ Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
    private void setStatus(String m){ status.setText(m); }

    private void openUrl(){
        String u = urlInput.getText().toString().trim();
        if(u.length()==0) return;
        if(!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u;
        web.loadUrl(u);
    }

    private String asset(String name){
        try(InputStream in=getAssets().open(name)){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while((n=in.read(buf))>0) out.write(buf,0,n);
            return out.toString("UTF-8");
        }catch(Exception e){ return "window.VisualCrawler.onError('asset load failed: " + e.getMessage().replace("'","") + "');"; }
    }

    private void enableListSelect(){
        if(collecting){ toast("수집 중에는 선택할 수 없어."); return; }
        web.evaluateJavascript(asset("select_list.js"), null);
    }

    private void enableBodySelect(){
        if(collecting){ toast("수집 중에는 선택할 수 없어."); return; }
        web.evaluateJavascript(asset("select_body.js"), null);
    }

    private void startCollect(){
        if(links.isEmpty()){ toast("먼저 목록영역을 선택해."); return; }
        if(bodySelector.length()==0){ toast("샘플 글에서 본문영역을 선택해."); return; }
        collected.setLength(0);
        outputText = "";
        currentIndex = 0;
        collecting = true;
        setStatus("전체수집 시작: 0 / " + links.size());
        web.loadUrl(links.get(0));
    }

    private void extractCurrentBody(){
        if(!collecting || currentIndex>=links.size()) return;
        String selector = JSONObject.quote(bodySelector);
        String js = "(function(){" +
                "function clean(t){return (t||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+/g,' ').replace(/\\n{3,}/g,'\\n\\n').trim();}" +
                "var el=null;try{el=document.querySelector(" + selector + ");}catch(e){}" +
                "var txt=el?clean(el.innerText||el.textContent):'';" +
                "window.VisualCrawler.onBodyExtracted(JSON.stringify({title:document.title||'',url:location.href,text:txt}));" +
                "})();";
        web.evaluateJavascript(js, null);
    }

    private void finishCollect(){
        collecting = false;
        outputText = collected.toString().trim();
        if(outputText.length()==0) outputText = "수집된 본문이 없어. 본문 선택 영역을 다시 잡아봐.";
        result.setText(outputText);
        setStatus("수집 완료: " + links.size() + "개 링크 처리. 다운로드 저장을 누르면 Download 폴더에 txt로 저장돼.");
        toast("수집 완료");
    }

    private void saveDownloads(){
        String txt = outputText.length()>0 ? outputText : result.getText().toString();
        if(txt.trim().length()==0 || txt.equals("결과가 여기에 표시돼.")){ toast("저장할 결과가 없어."); return; }
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date());
        String fileName = "visual_crawler_" + stamp + ".txt";
        try{
            if(Build.VERSION.SDK_INT >= 29){
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                cv.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                cv.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if(uri==null) throw new IOException("MediaStore insert failed");
                try(OutputStream os = getContentResolver().openOutputStream(uri)){
                    if(os==null) throw new IOException("OutputStream null");
                    os.write(txt.getBytes(StandardCharsets.UTF_8));
                }
                cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(uri, cv, null, null);
            }else{
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if(!dir.exists()) dir.mkdirs();
                try(FileOutputStream fos = new FileOutputStream(new File(dir, fileName))){ fos.write(txt.getBytes(StandardCharsets.UTF_8)); }
            }
            toast("Download 폴더에 저장됨: " + fileName);
            setStatus("저장 완료: Download/" + fileName);
        }catch(Exception e){ toast("저장 실패: " + e.getMessage()); setStatus("저장 실패: " + e.getMessage()); }
    }

    private void copyResult(){
        String txt = outputText.length()>0 ? outputText : result.getText().toString();
        ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("visual crawler", txt));
        toast("복사 완료");
    }

    private void resetState(){
        links.clear(); linkTitles.clear(); listPageUrl=""; bodySelector=""; outputText=""; collected.setLength(0); collecting=false; currentIndex=0;
        result.setText("결과가 여기에 표시돼.");
        setStatus("초기화 완료. 목록영역 선택부터 다시 시작해.");
    }

    private void showHelp(){
        new AlertDialog.Builder(this)
                .setTitle("사용법")
                .setMessage("1. 블로그 글 목록 페이지에서 '목록영역 선택'을 누르고 글 목록 전체를 감싸는 영역을 터치해.\n\n"+
                        "2. 앱이 그 영역 안의 글 링크를 기억해. 내부 글 페이지로 이동해도 기억은 유지돼.\n\n"+
                        "3. 목록 중 글 하나를 직접 눌러 들어간 뒤 '본문영역 선택'을 누르고 가져올 본문 부분을 터치해.\n\n"+
                        "4. '전체수집 시작'을 누르면 기억한 링크들을 순서대로 열고, 각 글에서 선택한 본문 영역의 보이는 텍스트만 가져와.\n\n"+
                        "5. '다운로드 저장'을 누르면 Download 폴더에 txt로 저장돼.")
                .setPositiveButton("확인", null).show();
    }

    @Override public void onBackPressed(){
        if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    public class Bridge {
        @JavascriptInterface public void onInfo(String msg){ runOnUiThread(() -> setStatus(msg + stateLine())); }
        @JavascriptInterface public void onError(String msg){ runOnUiThread(() -> { setStatus("오류: " + msg); toast("오류"); }); }

        @JavascriptInterface public void onListSelected(String raw){
            runOnUiThread(() -> {
                try{
                    JSONObject obj = new JSONObject(raw);
                    JSONArray arr = obj.getJSONArray("links");
                    links.clear(); linkTitles.clear();
                    for(int i=0;i<arr.length();i++){
                        JSONObject it = arr.getJSONObject(i);
                        String u = it.optString("url", "");
                        String t = it.optString("text", "");
                        if(u.length()>0 && !links.contains(u)){ links.add(u); linkTitles.add(t); }
                    }
                    listPageUrl = obj.optString("pageUrl", web.getUrl());
                    setStatus("목록영역 기억됨: " + links.size() + "개 링크. 이제 글 하나를 열고 본문영역을 선택해." + stateLine());
                    toast("목록 " + links.size() + "개 기억됨");
                }catch(Exception e){ setStatus("목록 파싱 실패: " + e.getMessage()); }
            });
        }

        @JavascriptInterface public void onBodySelected(String selector, String preview){
            runOnUiThread(() -> {
                bodySelector = selector;
                String p = preview==null ? "" : preview;
                if(p.length()>300) p=p.substring(0,300)+"...";
                result.setText("본문 선택 미리보기\n\n" + p);
                setStatus("본문영역 기억됨. 이제 전체수집 시작을 누르면 목록의 모든 글을 순회해." + stateLine());
                toast("본문영역 기억됨");
            });
        }

        @JavascriptInterface public void onBodyExtracted(String raw){
            runOnUiThread(() -> {
                try{
                    JSONObject o = new JSONObject(raw);
                    String title = o.optString("title", "").trim();
                    String url = o.optString("url", links.get(Math.min(currentIndex, links.size()-1)));
                    String text = o.optString("text", "").trim();
                    String listTitle = currentIndex<linkTitles.size()?linkTitles.get(currentIndex):"";
                    if(title.length()==0) title = listTitle.length()>0 ? listTitle : ("글 " + (currentIndex+1));
                    collected.append(title).append("\n").append(url).append("\n\n");
                    if(text.length()==0) collected.append("[본문을 찾지 못했어. 본문영역 선택자를 다시 잡아봐.]\n");
                    else collected.append(text).append("\n");
                    collected.append("\n------------------------------\n\n");
                    currentIndex++;
                    result.setText("수집 중: " + currentIndex + " / " + links.size() + "\n\n" + collected.toString());
                    setStatus("수집 중: " + currentIndex + " / " + links.size() + stateLine());
                    if(currentIndex < links.size()) web.loadUrl(links.get(currentIndex)); else finishCollect();
                }catch(Exception e){ collecting=false; setStatus("수집 오류: " + e.getMessage()); }
            });
        }
    }

    private String stateLine(){
        return "\n상태: 목록 " + links.size() + "개 기억됨 / 본문 " + (bodySelector.length()>0?"선택됨":"미선택");
    }
}
