package com.myapp.youtubelite.webview;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;

public class YTProWebViewClient extends WebViewClient {

    private static final String TAG = "YTProWebViewClient";
    private Context context;

    public YTProWebViewClient(Context context) {
        this.context = context;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString().toLowerCase();

        // Only block clearly ad-related URLs that won't affect video playback
        if (isAdUrl(url)) {
            Log.d(TAG, "Blocked ad URL: " + url);
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        }

        return super.shouldInterceptRequest(view, request);
    }

    private boolean isAdUrl(String url) {
        // Don't block if it's a video stream or player-related URL
        if (url.contains("googlevideo.com") ||
            url.contains("videoplayback") ||
            url.contains("/player") ||
            url.contains("youtubei/v1/player") ||
            url.contains("youtubei/v1/next") ||
            url.contains("youtubei/v1/browse") ||
            url.contains("youtubei/v1/search") ||
            url.contains("/watch") ||
            url.contains("yt3.ggpht.com") ||
            url.contains("i.ytimg.com") ||
            url.contains("youtube.com/s/") ||
            url.contains("youtube.com/youtubei")) {
            return false;
        }

        return url.contains("doubleclick.net") ||
                url.contains("googlesyndication.com") ||
                url.contains("googleadservices.com") ||
                url.contains("adservice.google.com") ||
                url.contains("ad.youtube.com") ||
                url.contains("youtube.com/api/ads") ||
                url.contains("youtube.com/pagead") ||
                url.contains("youtube.com/get_midroll") ||
                url.contains("adsystem.com") ||
                url.contains("admob.com") ||
                url.contains("ads.google.com") ||
                url.contains("fundingchoicesmessages.google.com") ||
                url.contains("tpc.googlesyndication.com") ||
                url.contains("securepubads") ||
                url.contains("imasdk.googleapis.com") ||
                url.contains("static.doubleclick.net") ||
                url.contains("s0.2mdn.net") ||
                url.contains("survey.g.doubleclick.net") ||
                url.contains("google.com/adsense");
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        view.evaluateJavascript(getAdBlockScript(), null);
        view.evaluateJavascript(getBackgroundPlaybackScript(), null);
        view.evaluateJavascript(getFastPlaybackScript(), null);
    }

    private String getFastPlaybackScript() {
        return "(" + "function() {" +
            "if (window._ytlFastPlaySetup) return;" +
            "window._ytlFastPlaySetup = true;" +

            // Force video to play as soon as it has enough data
            "function forcePlay() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) return;" +
            "  if (v.readyState >= 2 && v.paused && !window._ytlUserPaused) {" +
            "    v.play().catch(function(){});" +
            "  }" +
            "}" +

            // Watch for new video elements and force play
            "function watchVideo() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) { setTimeout(watchVideo, 500); return; }" +
            "  v.addEventListener('loadeddata', function() {" +
            "    if (!window._ytlUserPaused) { v.play().catch(function(){}); }" +
            "  });" +
            "  v.addEventListener('canplay', function() {" +
            "    if (!window._ytlUserPaused) { v.play().catch(function(){}); }" +
            "  });" +
            "  v.addEventListener('waiting', function() {" +
            "    setTimeout(forcePlay, 200);" +
            "  });" +
            "}" +
            "watchVideo();" +

            // Periodically check if video is stuck and force play
            "setInterval(function() {" +
            "  var v = document.querySelector('video');" +
            "  if (v && v.paused && !v.ended && !window._ytlUserPaused && v.readyState >= 2) {" +
            "    v.play().catch(function(){});" +
            "  }" +
            "}, 1000);" +

            // Re-watch on SPA navigation
            "var titleObs = new MutationObserver(function() { setTimeout(watchVideo, 500); });" +
            "var titleEl = document.querySelector('title') || document.head;" +
            "titleObs.observe(titleEl, {childList: true, subtree: true});" +
            "})();";
    }

    private String getAdBlockScript() {
        return "(" + "function() {" +
            // CSS to hide ad elements
            "var css = document.createElement('style');" +
            "css.textContent = '" +
            "ytm-promoted-sparkles-web-renderer," +
            "ytm-promoted-video-renderer," +
            "ytm-companion-ad-renderer," +
            "ytd-ad-slot-renderer," +
            "ytd-promoted-sparkles-web-renderer," +
            "ytd-promoted-video-renderer," +
            "ytd-display-ad-renderer," +
            "ytd-in-feed-ad-layout-renderer," +
            "ytd-banner-promo-renderer," +
            "ytd-statement-banner-renderer," +
            "ytd-carousel-ad-renderer," +
            "ytd-compact-promoted-video-renderer," +
            ".ad-container," +
            ".ad-showing," +
            ".ytp-ad-module," +
            ".ytp-ad-overlay-container," +
            ".ytp-ad-image-overlay," +
            ".ytp-ad-text-overlay," +
            ".video-ads," +
            "#masthead-ad," +
            "#player-ads," +
            "#ad-slot," +
            "ad-slot-renderer," +
            ".ytm-promoted-sparkles-web-renderer," +
            "[class*=\"ad-badge\"]," +
            "[class*=\"badge-style-type-ad\"]" +
            "{ display: none !important; height: 0 !important; overflow: hidden !important; }';" +
            "document.head.appendChild(css);" +

            // MutationObserver to remove ads dynamically
            "var adObserver = new MutationObserver(function(mutations) {" +
            "  mutations.forEach(function(mutation) {" +
            "    mutation.addedNodes.forEach(function(node) {" +
            "      if (node.nodeType !== 1) return;" +
            "      var tag = node.tagName ? node.tagName.toLowerCase() : '';" +
            "      if (tag.indexOf('ytd-ad') > -1 || tag.indexOf('ytm-promoted') > -1 || tag.indexOf('ytm-companion-ad') > -1 || tag === 'ytd-in-feed-ad-layout-renderer' || tag === 'ytd-carousel-ad-renderer') {" +
            "        node.remove(); return;" +
            "      }" +
            "      var txt = (node.textContent || '').toLowerCase();" +
            "      if ((txt.indexOf('sponsored') > -1 || txt.indexOf('visit advertiser') > -1) && txt.length < 2000) {" +
            "        var parent = node.closest('ytm-item-section-renderer, ytd-rich-item-renderer, ytm-rich-item-renderer, ytm-video-with-context-renderer, ytm-promoted-sparkles-web-renderer');" +
            "        if (parent) { parent.remove(); return; }" +
            "        if (tag.indexOf('ytm-') > -1 || tag.indexOf('ytd-') > -1) { node.remove(); return; }" +
            "      }" +
            "      var ads = node.querySelectorAll('ytm-promoted-sparkles-web-renderer, ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, ytm-companion-ad-renderer, ytd-carousel-ad-renderer');" +
            "      ads.forEach(function(a) { a.remove(); });" +
            "      var badges = node.querySelectorAll('[class*=\"badge-style-type-ad\"], [class*=\"ad-badge\"]');" +
            "      badges.forEach(function(b) {" +
            "        var p = b.closest('ytm-item-section-renderer, ytd-rich-item-renderer, ytm-rich-item-renderer');" +
            "        if (p) p.remove();" +
            "      });" +
            "    });" +
            "  });" +
            "});" +
            "adObserver.observe(document.documentElement, {childList: true, subtree: true});" +

            // Remove existing ads on page
            "function removeExistingAds() {" +
            "  var selectors = ['ytm-promoted-sparkles-web-renderer','ytd-ad-slot-renderer','ytd-in-feed-ad-layout-renderer','ytm-companion-ad-renderer','ytd-carousel-ad-renderer','ytd-promoted-video-renderer','ytd-display-ad-renderer','.ad-container','.ytp-ad-module','.video-ads','#masthead-ad','#player-ads'];" +
            "  selectors.forEach(function(s) {" +
            "    document.querySelectorAll(s).forEach(function(el) { el.remove(); });" +
            "  });" +
            "  document.querySelectorAll('[class*=\"badge-style-type-ad\"]').forEach(function(b) {" +
            "    var p = b.closest('ytm-item-section-renderer, ytd-rich-item-renderer, ytm-rich-item-renderer');" +
            "    if (p) p.remove();" +
            "  });" +
            "}" +
            "removeExistingAds();" +
            "setInterval(removeExistingAds, 2000);" +

            // Skip video ads immediately - faster interval
            "setInterval(function() {" +
            "  var v = document.querySelector('video');" +
            "  var adShowing = document.querySelector('.ad-showing, .ytp-ad-player-overlay');" +
            "  if (adShowing && v) {" +
            "    v.currentTime = v.duration || 9999;" +
            "    v.playbackRate = 16;" +
            "  }" +
            "  var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, [class*=\"skip-button\"], button[id*=\"skip\"]');" +
            "  if (skip) { skip.click(); }" +
            "}, 100);" +
            "})();";
    }

    private String getBackgroundPlaybackScript() {
        return "(" + "function() {" +
            // Override visibility API to trick YouTube into thinking page is always visible
            "Object.defineProperty(document, 'hidden', {get: function() { return false; }});" +
            "Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }});" +
            "document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);" +

            // Flag to track user-initiated pause from notification
            "window._ytlUserPaused = false;" +

            // Override pause to prevent YouTube from auto-pausing, but allow user-initiated pause
            "var origPause = HTMLVideoElement.prototype.pause;" +
            "HTMLVideoElement.prototype.pause = function() {" +
            "  if (window._ytlUserPaused) {" +
            "    window._ytlUserPaused = false;" +
            "    return origPause.apply(this, arguments);" +
            "  }" +
            "  return undefined;" +
            "};" +

            // Auto-play next video when current ends
            "function setupAutoplay() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) { setTimeout(setupAutoplay, 1000); return; }" +
            "  v.setAttribute('playsinline', '');" +
            "  v.setAttribute('webkit-playsinline', '');" +
            "  v.addEventListener('ended', function() {" +
            "    var next = document.querySelector('.ytp-next-button, .ytm-autonav-bar a, a.ytm-next-button, [class*=\"next-button\"]');" +
            "    if (next) { next.click(); }" +
            "    else {" +
            "      var related = document.querySelector('ytm-compact-video-renderer a, ytd-compact-video-renderer a');" +
            "      if (related) related.click();" +
            "    }" +
            "  });" +
            "}" +
            "setupAutoplay();" +

            // Re-setup on navigation (SPA)
            "var navObserver = new MutationObserver(function() { setTimeout(setupAutoplay, 1500); });" +
            "navObserver.observe(document.querySelector('title') || document.head, {childList: true, subtree: true});" +
            "})();";
    }
}
