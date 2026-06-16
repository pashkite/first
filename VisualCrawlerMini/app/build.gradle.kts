plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.visualcrawlerv05"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.visualcrawlerv05"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.6"
    }
}

tasks.configureEach {
    if (name == "preBuild") {
        doFirst {
            val f = project.file("src/main/java/com/example/visualcrawlerv05/MainActivity.java")
            var s = f.readText()
            if (!s.contains("CHROME_UA")) {
                s = s.replace(
                    "private final Handler handler = new Handler(Looper.getMainLooper());",
                    "private final Handler handler = new Handler(Looper.getMainLooper());\n    private static final String CHROME_UA = \"Mozilla/5.0 (Linux; Android 14; SM-S918N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36\";"
                )
                s = s.replace(
                    "Button help = btn(\"?\");",
                    "Button retry = btn(\"재시도\");\n        Button chrome = btn(\"Chrome\");\n        Button help = btn(\"?\");"
                )
                s = s.replace(
                    "help.setOnClickListener(v -> showHelp());",
                    "retry.setOnClickListener(v -> retryPage());\n        chrome.setOnClickListener(v -> openInChrome());\n        help.setOnClickListener(v -> showHelp());"
                )
                s = s.replace(
                    "top.addView(urlInput); top.addView(go); top.addView(help);",
                    "top.addView(urlInput); top.addView(go); top.addView(retry); top.addView(chrome); top.addView(help);"
                )
                s = s.replace(
                    "s.setLoadsImagesAutomatically(true);\n        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);",
                    "s.setLoadsImagesAutomatically(true);\n        s.setDatabaseEnabled(true);\n        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);\n        s.setJavaScriptCanOpenWindowsAutomatically(true);\n        s.setSupportMultipleWindows(true);\n        s.setUseWideViewPort(true);\n        s.setLoadWithOverviewMode(false);\n        s.setAllowContentAccess(true);\n        s.setAllowFileAccess(true);\n        s.setUserAgentString(CHROME_UA);\n        if(Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(false);\n        CookieManager cm = CookieManager.getInstance();\n        cm.setAcceptCookie(true);\n        if(Build.VERSION.SDK_INT >= 21) cm.setAcceptThirdPartyCookies(web, true);"
                )
                s = s.replace(
                    "web.setWebChromeClient(new WebChromeClient());",
                    "web.setWebChromeClient(new WebChromeClient(){\n            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg){\n                WebView.HitTestResult r = view.getHitTestResult();\n                String target = r != null ? r.getExtra() : null;\n                if(target != null && target.startsWith(\"http\")){ web.loadUrl(target); return false; }\n                return false;\n            }\n        });"
                )
                s = s.replace(
                    "@Override public void onPageFinished(WebView view, String url) {\n                super.onPageFinished(view, url);\n                if (collecting) handler.postDelayed(() -> extractCurrentBody(), 1200);\n            }",
                    "@Override public void onPageFinished(WebView view, String url) {\n                super.onPageFinished(view, url);\n                urlInput.setText(url);\n                if (collecting) handler.postDelayed(() -> extractCurrentBody(), 1500);\n            }\n            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err){\n                if(Build.VERSION.SDK_INT >= 23 && req != null && req.isForMainFrame()){\n                    setStatus(\"페이지 로드 실패: \" + err.getErrorCode() + \" / \" + err.getDescription() + \". 재시도 또는 Chrome 버튼을 눌러봐.\");\n                }\n            }\n            @SuppressWarnings(\"deprecation\") @Override public void onReceivedError(WebView view, int code, String desc, String failingUrl){\n                setStatus(\"페이지 로드 실패: \" + code + \" / \" + desc + \". 재시도 또는 Chrome 버튼을 눌러봐.\");\n            }\n            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req){ return false; }\n            @SuppressWarnings(\"deprecation\") @Override public boolean shouldOverrideUrlLoading(WebView view, String u){ return false; }"
                )
                s = s.replace(
                    "private String asset(String name){",
                    "private void retryPage(){ String u = web.getUrl(); if(u == null || u.trim().length()==0) u = urlInput.getText().toString().trim(); if(u != null && u.length()>0){ setStatus(\"재시도 중: \" + u); web.loadUrl(u); } }\n\n    private void openInChrome(){ try{ String u = web.getUrl(); if(u == null || u.trim().length()==0) u = urlInput.getText().toString().trim(); if(u == null || u.length()==0) return; if(!u.startsWith(\"http\")) u = \"https://\" + u; startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }catch(Exception e){ toast(\"Chrome 열기 실패\"); } }\n\n    private String asset(String name){"
                )
                s = s.replace(
                    "5. '다운로드 저장'을 누르면 Download 폴더에 txt로 저장돼.")",
                    "5. '다운로드 저장'을 누르면 Download 폴더에 txt로 저장돼.\\n\\n접속 오류가 뜨면 [재시도]를 먼저 누르고, Chrome에서는 열리는데 앱에서만 안 열리면 사이트가 WebView를 제한하는 경우일 수 있어. 이때 [Chrome]으로 확인해봐.")"
                )
                f.writeText(s)
            }
        }
    }
}
