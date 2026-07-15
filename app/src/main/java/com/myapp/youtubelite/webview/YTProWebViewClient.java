package com.myapp.youtubelite.webview;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.webkit.WebResourceRequestCompat;
import androidx.webkit.WebViewClientCompat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class YTProWebViewClient extends WebViewClientCompat {

    private static final String TAG = "YTProWebViewClient";
    private Context context;

    public YTProWebViewClient(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        // Ad blocking
        if (isAdUrl(url)) {
            Log.d(TAG, "Blocked ad URL: " + url);
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        }

        // CSP stripping (simplified - a full implementation would modify headers)
        // This is a placeholder for more complex CSP manipulation if needed.
        // For now, we'll focus on blocking ad requests.

        return super.shouldInterceptRequest(view, request);
    }

    private boolean isAdUrl(String url) {
        return url.contains("googleads") ||
                url.contains("doubleclick") ||
                url.contains("ad_break") ||
                url.contains("pagead");
    }

    // Method to inject JavaScript after page load (for ad object stripping and MediaSession polyfill)
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        // Inject ad-blocking JavaScript
        view.evaluateJavascript(getAdBlockingScript(), null);
        // Inject background playback JavaScript
        view.evaluateJavascript(getBackgroundPlaybackScript(), null);
    }

    private String getAdBlockingScript() {
        // This script will intercept fetch/XHR and strip ad objects from YouTube API JSON responses
        // This is a simplified version, a full implementation would be more robust.
        return "" +
                "(function() {" +
                "    var originalFetch = window.fetch;" +
                "    window.fetch = function() {" +
                "        var url = arguments[0].toString();" +
                "        if (url.includes('googleads') || url.includes('doubleclick') || url.includes('ad_break') || url.includes('pagead')) {" +
                "            console.log('Blocked fetch request: ' + url);" +
                "            return Promise.resolve(new Response(''));" +
                "        }" +
                "        return originalFetch.apply(this, arguments).then(response => {" +
                "            if (url.includes('youtubei/v1')) {" +
                "                return response.clone().json().then(json => {" +
                "                    if (json.adSlots) delete json.adSlots;" +
                "                    if (json.playerAds) delete json.playerAds;" +
                "                    if (json.adPlacements) delete json.adPlacements;" +
                "                    if (json.adBreakHeartbeatParams) delete json.adBreakHeartbeatParams;" +
                "                    return new Response(JSON.stringify(json), {headers: response.headers});" +
                "                }).catch(() => response);" +
                "            }" +
                "            return response;" +
                "        });" +
                "    };" +
                "    var originalXHRopen = XMLHttpRequest.prototype.open;" +
                "    XMLHttpRequest.prototype.open = function(method, url) {
" +
                "        if (url.includes('googleads') || url.includes('doubleclick') || url.includes('ad_break') || url.includes('pagead')) {" +
                "            console.log('Blocked XHR request: ' + url);" +
                "            this._blocked = true;" +
                "        }" +
                "        return originalXHRopen.apply(this, arguments);" +
                "    };" +
                "    var originalXHRsend = XMLHttpRequest.prototype.send;" +
                "    XMLHttpRequest.prototype.send = function() {" +
                "        if (this._blocked) {" +
                "            console.log('Prevented sending blocked XHR request.');" +
                "            return;" +
                "        }" +
                "        return originalXHRsend.apply(this, arguments);" +
                "    };" +
                "})();";
    }

    private String getBackgroundPlaybackScript() {
        // This script will polyfill MediaSession API and communicate with Android via JS bridge
        return "" +
                "(function() {" +
                "    if ('mediaSession' in navigator) {" +
                "        navigator.mediaSession.setActionHandler('play', function() { Android.play(); });" +
                "        navigator.mediaSession.setActionHandler('pause', function() { Android.pause(); });" +
                "        navigator.mediaSession.setActionHandler('nexttrack', function() { Android.nextTrack(); });" +
                "        navigator.mediaSession.setActionHandler('previoustrack', function() { Android.previousTrack(); });" +
                "    }" +
                "})();";
    }
}
