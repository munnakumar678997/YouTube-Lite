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

        // Only block clearly ad-serving URLs that won't affect video playback
        if (isAdUrl(url)) {
            Log.d(TAG, "Blocked ad URL: " + url);
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        }

        return super.shouldInterceptRequest(view, request);
    }

    private boolean isAdUrl(String url) {
        // NEVER block these - they are essential for video playback
        if (url.contains("googlevideo.com") ||
            url.contains("videoplayback") ||
            url.contains("youtubei/v1/player") ||
            url.contains("youtubei/v1/next") ||
            url.contains("youtubei/v1/browse") ||
            url.contains("youtubei/v1/search") ||
            url.contains("youtubei/v1/guide") ||
            url.contains("youtube.com/watch") ||
            url.contains("youtube.com/results") ||
            url.contains("youtube.com/shorts") ||
            url.contains("yt3.ggpht.com") ||
            url.contains("i.ytimg.com") ||
            url.contains("youtube.com/s/") ||
            url.contains("youtube.com/youtubei") ||
            url.contains("youtube.com/embed") ||
            url.contains("youtube.com/api/stats") ||
            url.contains("youtube.com/ptracking") ||
            url.contains("youtube.com/api/timedtext") ||
            url.contains("gstatic.com")) {
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
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        // For YouTube URLs, let the WebView handle it (SPA navigation)
        // But re-inject scripts after a short delay to handle new video
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.evaluateJavascript(getAdBlockScript(), null);
                    view.evaluateJavascript(getBackgroundPlaybackScript(), null);
                }
            }, 500);
            return false;
        }
        return false;
    }

    private String getAdBlockScript() {
        return "(" + "function() {" +

            // CSS to hide ad elements (always re-apply, no guard)
            "if (!window._ytlAdCssAdded) {" +
            "  window._ytlAdCssAdded = true;" +
            "  var css = document.createElement('style');" +
            "  css.textContent = '" +
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
            "  document.head.appendChild(css);" +
            "}" +

            // Main ad skip function - works on both desktop and mobile YouTube
            "function skipAds() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) return;" +
            "  var adShowing = false;" +

            // Method 1: .ad-showing class on player (desktop YouTube)
            "  var player = document.querySelector('.html5-video-player');" +
            "  if (player && player.classList.contains('ad-showing')) adShowing = true;" +

            // Method 2: Ad overlay elements (both mobile and desktop)
            "  if (!adShowing && document.querySelector('.ytp-ad-player-overlay, .ytp-ad-player-overlay-instream-info, .ytp-ad-text, .ytp-ad-preview-container, .ytp-ad-skip-button-slot, .ytp-ad-message-container')) adShowing = true;" +

            // Method 3: For mobile YouTube (m.youtube.com) - check ad-related attributes
            "  if (!adShowing && document.querySelector('[class*=\"ad-interrupting\"], [class*=\"ad-created\"], .player-ads-container, ytm-promoted-sparkles-web-renderer')) adShowing = true;" +

            // Method 4: Check video src for ad indicators
            "  if (!adShowing && v.src && (v.src.indexOf('&ctier=') > -1 || v.src.indexOf('oad/') > -1)) adShowing = true;" +

            // Method 5: If video duration is very short (< 60s) and different from expected content
            // This catches server-side injected ads that show as separate short videos
            "  if (!adShowing && v.duration > 0 && v.duration <= 31 && window._ytlExpectedLongVideo) adShowing = true;" +

            "  if (adShowing) {" +
            "    v.currentTime = v.duration ? v.duration - 0.1 : 9999;" +
            "    v.playbackRate = 16;" +
            "    var skipBtns = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, [class*=\"skip-button\"], .ytp-ad-skip-button-container button, .videoAdUiSkipButton, button[id*=\"skip\"]');" +
            "    skipBtns.forEach(function(b) { b.click(); });" +
            "  } else {" +
            "    if (v.playbackRate > 1 && v.playbackRate === 16) v.playbackRate = 1;" +
            // Track that we expect a long video (content, not ad)
            "    if (v.duration > 60) window._ytlExpectedLongVideo = true;" +
            "  }" +
            "}" +

            // Run ad skip very frequently - 50ms
            "if (!window._ytlAdSkipInterval) {" +
            "  window._ytlAdSkipInterval = setInterval(skipAds, 50);" +
            "}" +

            // Watch for video element changes (new video loaded = SPA navigation)
            "function watchVideoChanges() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) { setTimeout(watchVideoChanges, 500); return; }" +
            "  if (v._ytlWatched) return;" +
            "  v._ytlWatched = true;" +

            // When video source changes, reset expected duration flag and check for ads
            "  v.addEventListener('loadstart', function() {" +
            "    window._ytlExpectedLongVideo = false;" +
            "    setTimeout(skipAds, 100);" +
            "    setTimeout(skipAds, 300);" +
            "    setTimeout(skipAds, 500);" +
            "    setTimeout(skipAds, 1000);" +
            "    setTimeout(skipAds, 2000);" +
            "  });" +

            // When metadata loads, check duration for ad detection
            "  v.addEventListener('loadedmetadata', function() {" +
            "    skipAds();" +
            "  });" +

            // Force play when data is available
            "  v.addEventListener('canplay', function() {" +
            "    if (!window._ytlUserPaused && v.paused) {" +
            "      v.play().catch(function(){});" +
            "    }" +
            "  });" +
            "}" +
            "watchVideoChanges();" +

            // MutationObserver to catch ads immediately when they appear
            "if (!window._ytlAdMutObsSetup) {" +
            "  window._ytlAdMutObsSetup = true;" +
            "  var adMutObs = new MutationObserver(function(mutations) {" +
            "    for (var i = 0; i < mutations.length; i++) {" +
            "      var m = mutations[i];" +
            "      if (m.type === 'attributes' && m.attributeName === 'class') {" +
            "        var t = m.target;" +
            "        if (t.classList && (t.classList.contains('ad-showing') || t.classList.contains('ad-interrupting'))) {" +
            "          skipAds();" +
            "        }" +
            "      }" +
            "      if (m.type === 'childList') {" +
            "        m.addedNodes.forEach(function(node) {" +
            "          if (node.nodeType === 1) {" +
            "            if (node.classList && (node.classList.contains('ytp-ad-module') || node.classList.contains('video-ads'))) {" +
            "              skipAds();" +
            "            }" +
            "            if (node.tagName === 'VIDEO') {" +
            "              watchVideoChanges();" +
            "            }" +
            "          }" +
            "        });" +
            "      }" +
            "    }" +
            "  });" +
            "  adMutObs.observe(document.documentElement, {childList: true, subtree: true, attributes: true, attributeFilter: ['class']});" +
            "}" +

            // Remove existing feed ads
            "function removeExistingAds() {" +
            "  var selectors = ['ytm-promoted-sparkles-web-renderer','ytd-ad-slot-renderer','ytd-in-feed-ad-layout-renderer','ytm-companion-ad-renderer','ytd-carousel-ad-renderer','ytd-promoted-video-renderer','ytd-display-ad-renderer','.ad-container','.ytp-ad-module','.video-ads','#masthead-ad','#player-ads'];" +
            "  selectors.forEach(function(s) {" +
            "    document.querySelectorAll(s).forEach(function(el) { el.remove(); });" +
            "  });" +
            "}" +
            "removeExistingAds();" +
            "if (!window._ytlRemoveAdsInterval) {" +
            "  window._ytlRemoveAdsInterval = setInterval(removeExistingAds, 2000);" +
            "}" +

            // Intercept fetch to strip ad data from API responses
            "if (!window._ytlFetchIntercepted) {" +
            "  window._ytlFetchIntercepted = true;" +
            "  var origFetch = window.fetch;" +
            "  window.fetch = function() {" +
            "    var u = (arguments[0] && arguments[0].url) ? arguments[0].url : String(arguments[0]);" +
            "    if (u.indexOf('doubleclick') > -1 || u.indexOf('googlesyndication') > -1 || u.indexOf('imasdk') > -1) {" +
            "      return Promise.resolve(new Response('', {status: 200}));" +
            "    }" +
            "    return origFetch.apply(this, arguments).then(function(resp) {" +
            "      if (u.indexOf('youtubei/v1/player') > -1) {" +
            "        return resp.clone().text().then(function(t) {" +
            "          try {" +
            "            var j = JSON.parse(t);" +
            "            delete j.adSlots; delete j.playerAds; delete j.adPlacements;" +
            "            delete j.adBreakHeartbeatParams; delete j.adBreakParams;" +
            "            if (j.playerResponse) {" +
            "              delete j.playerResponse.adPlacements;" +
            "              delete j.playerResponse.playerAds;" +
            "              delete j.playerResponse.adSlots;" +
            "              delete j.playerResponse.adBreakParams;" +
            "            }" +
            "            return new Response(JSON.stringify(j), {status: resp.status, headers: resp.headers});" +
            "          } catch(e) { return resp; }" +
            "        });" +
            "      }" +
            "      return resp;" +
            "    });" +
            "  };" +
            "}" +

            // Intercept XHR for player requests to strip ads
            "if (!window._ytlXHRIntercepted) {" +
            "  window._ytlXHRIntercepted = true;" +
            "  var origOpen = XMLHttpRequest.prototype.open;" +
            "  XMLHttpRequest.prototype.open = function(m, u) {" +
            "    this._url = u || '';" +
            "    if (u && (u.indexOf('doubleclick') > -1 || u.indexOf('googlesyndication') > -1)) {" +
            "      this._blocked = true;" +
            "    }" +
            "    return origOpen.apply(this, arguments);" +
            "  };" +
            "  var origSend = XMLHttpRequest.prototype.send;" +
            "  XMLHttpRequest.prototype.send = function() {" +
            "    if (this._blocked) return;" +
            "    var self = this;" +
            "    if (self._url && self._url.indexOf('youtubei/v1/player') > -1) {" +
            "      var origOnReady = self.onreadystatechange;" +
            "      self.onreadystatechange = function() {" +
            "        if (self.readyState === 4) {" +
            "          try {" +
            "            var j = JSON.parse(self.responseText);" +
            "            delete j.adSlots; delete j.playerAds; delete j.adPlacements;" +
            "            delete j.adBreakHeartbeatParams; delete j.adBreakParams;" +
            "            Object.defineProperty(self, 'responseText', {value: JSON.stringify(j), writable: false});" +
            "            Object.defineProperty(self, 'response', {value: JSON.stringify(j), writable: false});" +
            "          } catch(e) {}" +
            "        }" +
            "        if (origOnReady) origOnReady.apply(self, arguments);" +
            "      };" +
            "    }" +
            "    return origSend.apply(this, arguments);" +
            "  };" +
            "}" +

            "})();";
    }

    private String getBackgroundPlaybackScript() {
        return "(" + "function() {" +
            "if (window._ytlBgPlaySetup) return;" +
            "window._ytlBgPlaySetup = true;" +

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
            "  if (v._ytlAutoplaySetup) return;" +
            "  v._ytlAutoplaySetup = true;" +
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

            // Re-setup on navigation (SPA) - watch for URL changes
            "var lastUrl = location.href;" +
            "setInterval(function() {" +
            "  if (location.href !== lastUrl) {" +
            "    lastUrl = location.href;" +
            "    window._ytlExpectedLongVideo = false;" +
            "    setTimeout(setupAutoplay, 1000);" +
            "  }" +
            "}, 500);" +

            // Also watch title changes
            "var navObserver = new MutationObserver(function() { setTimeout(setupAutoplay, 1500); });" +
            "navObserver.observe(document.querySelector('title') || document.head, {childList: true, subtree: true});" +
            "})();";
    }
}
