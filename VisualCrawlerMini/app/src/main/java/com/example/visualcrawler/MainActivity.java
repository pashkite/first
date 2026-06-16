package com.example.visualcrawler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private EditText urlInput;
    private TextView statusText;
    private TextView resultText;
    private String lastText = "";
    private String lastRules = "{}";
    private SharedPreferences prefs;
    private JSONArray pending;
    private JSONArray done;
    private int detailIndex = 0;
    private String bodySelector = "";
    private String listUrl = "";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("rules", MODE_PRIVATE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 8, 8, 8);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://example.com");
        urlInput.setText("https://example.com");
        urlInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button go = button("접속", () -> loadUrl(urlInput.getText().toString()));
        Button help = button("?", this::showHelp);
        top.addView(urlInput); top.addView(go); top.addView(help);
        root.addView(top);

        LinearLayout r1 = row();
        r1.addView(button("카드 선택", () -> pick("card")));
        r1.addView(button("제목 선택", () -> pick("title")));
        r1.addView(button("링크글 선택", () -> pick("link")));
        root.addView(r1);
        LinearLayout r2 = row();
        r2.addView(button("날짜 선택", () -> pick("date")));
        r2.addView(button("요약 선택", () -> pick("summary")));
        r2.addView(button("본문 선택", () -> pick("body")));
        root.addView(r2);
        LinearLayout r3 = row();
        r3.addView(button("추출", this::extract));
        r3.addView(button("규칙 저장", this::saveRules));
        r3.addView(button("규칙 불러오기", this::loadRules));
        root.addView(r3);
        LinearLayout r4 = row();
        r4.addView(button("결과 복사", this::copyResult));
        r4.addView(button("다운로드 저장", this::saveDownload));
        r4.addView(button("결과 지우기", () -> { lastText = ""; resultText.setText(""); }));
        root.addView(r4);

        statusText = new TextView(this);
        statusText.setText("? 버튼을 눌러 사용법을 확인하세요.");
        root.addView(statusText);

        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadsImagesAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView v, String url) {
                inject();
                if (pending != null) run("VC.extractBody(" + JSONObject.quote(bodySelector) + ")");
            }
        });
        webView.addJavascriptInterface(new Bridge(), "VisualCrawler");
        root.addView(webView);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 260));
        resultText = new TextView(this);
        resultText.setTextSize(13f);
        resultText.setPadding(8, 8, 8, 8);
        sv.addView(resultText);
        root.addView(sv);
        setContentView(root);
        loadUrl(urlInput.getText().toString());
    }

    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private Button button(String t, final Runnable r) { Button b = new Button(this); b.setText(t); b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)); b.setOnClickListener(v -> r.run()); return b; }
    private void loadUrl(String raw) { if (raw == null || raw.trim().isEmpty()) return; String u = raw.trim(); if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u; webView.loadUrl(u); }
    private void inject() { run(loadAsset("selector.js")); }
    private void run(String js) { webView.evaluateJavascript(js, null); }
    private void pick(String mode) { inject(); run("VC.setRules(" + JSONObject.quote(lastRules) + "); VC.pick(" + JSONObject.quote(mode) + ")"); }
    private void extract() { listUrl = webView.getUrl(); inject(); run("VC.setRules(" + JSONObject.quote(lastRules) + "); VC.extractList()"); }

    private String loadAsset(String name) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(name), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line; while ((line = br.readLine()) != null) sb.append(line).append('\n'); return sb.toString();
        } catch (Exception e) { return "window.VisualCrawler.onStatus('JS 로드 실패')"; }
    }

    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("사용법")
            .setMessage("1. 블로그 목록으로 접속합니다.\n2. 카드 선택을 누르고 글 카드 하나를 터치합니다.\n3. 같은 카드 안에서 제목/링크글/날짜/요약을 각각 선택합니다.\n4. 글 하나를 직접 열고 본문 선택을 눌러 본문 영역을 터치합니다.\n5. 목록으로 돌아와 추출을 누릅니다.\n6. 다운로드 저장을 누르면 Download/다운로드 폴더에 TXT로 저장됩니다.\n\n결과에는 HTML 태그가 아니라 화면에 보이는 글만 들어갑니다.")
            .setPositiveButton("확인", null).show();
    }

    private void saveRules() { prefs.edit().putString("rules", lastRules).apply(); toast("규칙 저장 완료"); }
    private void loadRules() { lastRules = prefs.getString("rules", "{}"); inject(); run("VC.setRules(" + JSONObject.quote(lastRules) + ")"); toast("규칙 불러오기 완료"); }
    private void copyResult() { ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("visual-crawler", lastText)); toast("복사 완료"); }

    private void startDetails(JSONArray arr, String selector) {
        pending = arr; done = new JSONArray(); detailIndex = 0; bodySelector = selector == null ? "" : selector;
        status("본문 수집 시작: " + arr.length() + "개"); loadNextDetail();
    }
    private void loadNextDetail() {
        try {
            if (pending == null || detailIndex >= pending.length()) { finishDetails(); return; }
            String u = pending.getJSONObject(detailIndex).optString("detailUrl", "");
            if (u.isEmpty()) { done.put(pending.getJSONObject(detailIndex)); detailIndex++; loadNextDetail(); return; }
            webView.loadUrl(u);
        } catch(Exception e) { finishDetails(); }
    }
    private void finishDetails() { pending = null; format(done); if (listUrl != null && !listUrl.isEmpty()) webView.loadUrl(listUrl); status("추출 완료"); }

    private void format(JSONArray arr) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<arr.length();i++) try {
            JSONObject o = arr.getJSONObject(i);
            if (i>0) sb.append("\n\n------------------------------\n\n");
            add(sb, "제목", o.optString("title"));
            add(sb, "링크글", o.optString("linkText"));
            add(sb, "날짜", o.optString("date"));
            add(sb, "요약", o.optString("summary"));
            add(sb, "본문", o.optString("body"));
        } catch(Exception ignored) {}
        lastText = sb.toString().trim(); resultText.setText(lastText);
    }
    private void add(StringBuilder sb, String k, String v) { if (v != null && !v.trim().isEmpty()) sb.append(k).append(":\n").append(v.trim()).append("\n\n"); }

    private void saveDownload() {
        if (lastText.trim().isEmpty()) { toast("저장할 결과가 없어"); return; }
        String name = "visual_crawler_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date()) + ".txt";
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name); v.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain"); v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                ContentResolver r = getContentResolver(); Uri uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("저장 위치 생성 실패");
                try (OutputStream os = r.openOutputStream(uri)) { os.write(lastText.getBytes(StandardCharsets.UTF_8)); }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); if (!dir.exists()) dir.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(new File(dir, name))) { fos.write(lastText.getBytes(StandardCharsets.UTF_8)); }
            }
            toast("다운로드 폴더 저장 완료: " + name);
        } catch(Exception e) { toast("저장 실패: " + e.getMessage()); }
    }
    private void status(String m) { statusText.setText(m); }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    public class Bridge {
        @JavascriptInterface public void onStatus(String m) { runOnUiThread(() -> status(m)); }
        @JavascriptInterface public void onPicked(String json) { runOnUiThread(() -> { try { JSONObject o = new JSONObject(json); JSONObject r = new JSONObject(lastRules); r.put(o.getString("mode"), o.getString("selector")); lastRules = r.toString(); status(o.getString("mode") + " 선택됨"); } catch(Exception e) { status("선택 오류"); } }); }
        @JavascriptInterface public void onExtracted(String json) { runOnUiThread(() -> { try { JSONObject o = new JSONObject(json); JSONArray arr = o.getJSONArray("items"); String bs = o.optString("bodySelector", ""); if (!bs.isEmpty()) startDetails(arr, bs); else { format(arr); status("목록 추출 완료"); } } catch(Exception e) { status("추출 오류: " + e.getMessage()); } }); }
        @JavascriptInterface public void onBodyExtracted(String body) { runOnUiThread(() -> { try { JSONObject o = pending.getJSONObject(detailIndex); o.put("body", body); done.put(o); detailIndex++; loadNextDetail(); } catch(Exception e) { finishDetails(); } }); }
    }
}
