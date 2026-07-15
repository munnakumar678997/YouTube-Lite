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

        if (isAdUrl(url)) {
            Log.d(TAG, "Blocked ad URL: " + url);
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        }

        return super.shouldInterceptRequest(view, request);
    }

    private boolean isAdUrl(String url) {
        // NEVER block video/player/content URLs
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
        view.evaluateJavascript(getMasterScript(), null);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.evaluateJavascript(getMasterScript(), null);
                }
            }, 300);
            return false;
        }
        return false;
    }

    private String getMasterScript() {
        return "(" + "function() {" +
            "if (window._ytlMasterV3) return;" +
            "window._ytlMasterV3 = true;" +

            // ============ CSS AD HIDE ============
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
            "[class*=\"ad-badge\"]," +
            "[class*=\"badge-style-type-ad\"]" +
            "{ display: none !important; height: 0 !important; overflow: hidden !important; }';" +
            "document.head.appendChild(css);" +

            // ============ BACKGROUND PLAYBACK ============
            "Object.defineProperty(document, 'hidden', {get: function() { return false; }, configurable: true});" +
            "Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            "document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);" +
            "window._ytlUserPaused = false;" +
            "var origPause = HTMLVideoElement.prototype.pause;" +
            "HTMLVideoElement.prototype.pause = function() {" +
            "  if (window._ytlUserPaused) {" +
            "    window._ytlUserPaused = false;" +
            "    return origPause.apply(this, arguments);" +
            "  }" +
            "  return undefined;" +
            "};" +

            // ============ AD SKIP LOGIC ============
            "function isAdPlaying() {" +
            "  var player = document.querySelector('.html5-video-player');" +
            "  if (player && player.classList.contains('ad-showing')) return true;" +
            "  if (document.querySelector('.ytp-ad-player-overlay, .ytp-ad-player-overlay-instream-info, .ytp-ad-text, .ytp-ad-preview-container, .ytp-ad-skip-button-slot, .ytp-ad-message-container')) return true;" +
            "  if (document.querySelector('[class*=\"ad-interrupting\"], [class*=\"ad-created\"], .player-ads-container')) return true;" +
            "  return false;" +
            "}" +

            "function skipAd() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) return;" +
            "  if (isAdPlaying()) {" +
            "    v.currentTime = v.duration ? v.duration - 0.1 : 9999;" +
            "    v.playbackRate = 16;" +
            "    var skipBtns = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, [class*=\"skip-button\"], .ytp-ad-skip-button-container button, .videoAdUiSkipButton');" +
            "    skipBtns.forEach(function(b) { b.click(); });" +
            "    return true;" +
            "  }" +
            "  if (v.playbackRate === 16) v.playbackRate = 1;" +
            "  return false;" +
            "}" +
            "setInterval(skipAd, 50);" +

            // ============ STALL DETECTION & FORCE PLAY ============
            // This is the KEY fix for the black screen on 2nd video
            // When YouTube navigates to a new video (SPA), the video element
            // often gets stuck in a loading/stalled state. We detect this and
            // force the video to play by clicking the player or calling play()
            "var _stallCheckStart = 0;" +
            "var _lastVideoTime = -1;" +
            "var _lastVideoSrc = '';" +

            "function checkStall() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) return;" +
            "  if (window._ytlUserPaused) return;" +
            "  if (isAdPlaying()) return;" +  // Don't interfere with ad skip

            // Detect if video source changed (new video navigation)
            "  var currentSrc = v.currentSrc || v.src || '';" +
            "  if (currentSrc !== _lastVideoSrc) {" +
            "    _lastVideoSrc = currentSrc;" +
            "    _stallCheckStart = Date.now();" +
            "    _lastVideoTime = -1;" +
            "  }" +

            // Check if video is stuck (paused or not progressing)
            "  var isStuck = false;" +
            "  if (v.paused && !v.ended && v.readyState >= 2) {" +
            "    isStuck = true;" +
            "  }" +
            "  if (!v.paused && v.currentTime === _lastVideoTime && v.readyState < 3) {" +
            "    isStuck = true;" +
            "  }" +
            "  _lastVideoTime = v.currentTime;" +

            // If stuck for more than 2 seconds, force play
            "  if (isStuck && (Date.now() - _stallCheckStart) > 2000) {" +
            "    v.play().catch(function(){});" +
            // If still stuck after play attempt, try clicking the player
            "    setTimeout(function() {" +
            "      var v2 = document.querySelector('video');" +
            "      if (v2 && v2.paused && !v2.ended && !window._ytlUserPaused) {" +
            "        var playerEl = document.querySelector('.html5-video-player, .player-container, #player');" +
            "        if (playerEl) playerEl.click();" +
            "        v2.play().catch(function(){});" +
            "      }" +
            "    }, 500);" +
            "  }" +

            // If video has been black/stuck for 5+ seconds, try reloading video source
            "  if (isStuck && (Date.now() - _stallCheckStart) > 5000) {" +
            "    if (v.readyState < 2) {" +
            // Video hasn't loaded enough data - try to trigger reload
            "      var src = v.currentSrc || v.src;" +
            "      if (src) {" +
            "        v.load();" +
            "        v.play().catch(function(){});" +
            "      }" +
            "    }" +
            "  }" +

            // If stuck for 8+ seconds, most aggressive - seek slightly and play
            "  if (isStuck && (Date.now() - _stallCheckStart) > 8000) {" +
            "    v.currentTime = 0.1;" +
            "    v.play().catch(function(){});" +
            "    _stallCheckStart = Date.now();" + // Reset to avoid infinite loop
            "  }" +
            "}" +
            "setInterval(checkStall, 500);" +

            // ============ VIDEO CHANGE WATCHER ============
            // Watch for new video loads and reset stall timer
            "function attachVideoListeners() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v || v._ytlListenersAttached) return;" +
            "  v._ytlListenersAttached = true;" +
            "  v.addEventListener('loadstart', function() {" +
            "    _stallCheckStart = Date.now();" +
            "    _lastVideoTime = -1;" +
            "  });" +
            "  v.addEventListener('playing', function() {" +
            "    _stallCheckStart = Date.now();" +
            "    if (v.playbackRate === 16 && !isAdPlaying()) v.playbackRate = 1;" +
            "  });" +
            "  v.addEventListener('canplay', function() {" +
            "    if (!window._ytlUserPaused && v.paused && !isAdPlaying()) {" +
            "      v.play().catch(function(){});" +
            "    }" +
            "  });" +
            "}" +
            "attachVideoListeners();" +

            // ============ SPA NAVIGATION WATCHER ============
            "var _lastUrl = location.href;" +
            "setInterval(function() {" +
            "  if (location.href !== _lastUrl) {" +
            "    _lastUrl = location.href;" +
            "    _stallCheckStart = Date.now();" +
            "    _lastVideoTime = -1;" +
            "    setTimeout(attachVideoListeners, 500);" +
            "    setTimeout(function() {" +
            "      var v = document.querySelector('video');" +
            "      if (v && v.paused && !window._ytlUserPaused) {" +
            "        v.play().catch(function(){});" +
            "      }" +
            "    }, 1000);" +
            "    setTimeout(function() {" +
            "      var v = document.querySelector('video');" +
            "      if (v && v.paused && !window._ytlUserPaused) {" +
            "        v.play().catch(function(){});" +
            "      }" +
            "    }, 2000);" +
            "    setTimeout(function() {" +
            "      var v = document.querySelector('video');" +
            "      if (v && v.paused && !window._ytlUserPaused) {" +
            "        v.play().catch(function(){});" +
            "        v.load();" +
            "        v.play().catch(function(){});" +
            "      }" +
            "    }, 4000);" +
            "  }" +
            "}, 300);" +

            // ============ MUTATION OBSERVER ============
            "var obs = new MutationObserver(function(mutations) {" +
            "  for (var i = 0; i < mutations.length; i++) {" +
            "    var m = mutations[i];" +
            "    if (m.type === 'attributes' && m.attributeName === 'class') {" +
            "      if (m.target.classList && m.target.classList.contains('ad-showing')) {" +
            "        skipAd();" +
            "      }" +
            "    }" +
            "    if (m.type === 'childList') {" +
            "      m.addedNodes.forEach(function(node) {" +
            "        if (node.nodeType === 1 && node.tagName === 'VIDEO') {" +
            "          attachVideoListeners();" +
            "        }" +
            "      });" +
            "    }" +
            "  }" +
            "});" +
            "obs.observe(document.documentElement, {childList: true, subtree: true, attributes: true, attributeFilter: ['class']});" +

            // ============ FEED AD REMOVAL ============
            "function removeExistingAds() {" +
            "  var selectors = ['ytm-promoted-sparkles-web-renderer','ytd-ad-slot-renderer','ytd-in-feed-ad-layout-renderer','ytm-companion-ad-renderer','ytd-carousel-ad-renderer','ytd-promoted-video-renderer','ytd-display-ad-renderer','.ad-container','.ytp-ad-module','.video-ads','#masthead-ad','#player-ads'];" +
            "  selectors.forEach(function(s) {" +
            "    document.querySelectorAll(s).forEach(function(el) { el.remove(); });" +
            "  });" +
            "}" +
            "removeExistingAds();" +
            "setInterval(removeExistingAds, 2000);" +

            // ============ FETCH/XHR INTERCEPT ============
            "var origFetch = window.fetch;" +
            "window.fetch = function() {" +
            "  var u = (arguments[0] && arguments[0].url) ? arguments[0].url : String(arguments[0]);" +
            "  if (u.indexOf('doubleclick') > -1 || u.indexOf('googlesyndication') > -1 || u.indexOf('imasdk') > -1) {" +
            "    return Promise.resolve(new Response('', {status: 200}));" +
            "  }" +
            "  return origFetch.apply(this, arguments).then(function(resp) {" +
            "    if (u.indexOf('youtubei/v1/player') > -1) {" +
            "      return resp.clone().text().then(function(t) {" +
            "        try {" +
            "          var j = JSON.parse(t);" +
            "          delete j.adSlots; delete j.playerAds; delete j.adPlacements;" +
            "          delete j.adBreakHeartbeatParams; delete j.adBreakParams;" +
            "          if (j.playerResponse) {" +
            "            delete j.playerResponse.adPlacements;" +
            "            delete j.playerResponse.playerAds;" +
            "            delete j.playerResponse.adSlots;" +
            "            delete j.playerResponse.adBreakParams;" +
            "          }" +
            "          return new Response(JSON.stringify(j), {status: resp.status, headers: resp.headers});" +
            "        } catch(e) { return resp; }" +
            "      });" +
            "    }" +
            "    return resp;" +
            "  });" +
            "};" +

            "var origOpen = XMLHttpRequest.prototype.open;" +
            "XMLHttpRequest.prototype.open = function(m, u) {" +
            "  this._url = u || '';" +
            "  if (u && (u.indexOf('doubleclick') > -1 || u.indexOf('googlesyndication') > -1)) {" +
            "    this._blocked = true;" +
            "  }" +
            "  return origOpen.apply(this, arguments);" +
            "};" +
            "var origSend = XMLHttpRequest.prototype.send;" +
            "XMLHttpRequest.prototype.send = function() {" +
            "  if (this._blocked) return;" +
            "  var self = this;" +
            "  if (self._url && self._url.indexOf('youtubei/v1/player') > -1) {" +
            "    var origOnReady = self.onreadystatechange;" +
            "    self.onreadystatechange = function() {" +
            "      if (self.readyState === 4) {" +
            "        try {" +
            "          var j = JSON.parse(self.responseText);" +
            "          delete j.adSlots; delete j.playerAds; delete j.adPlacements;" +
            "          delete j.adBreakHeartbeatParams; delete j.adBreakParams;" +
            "          Object.defineProperty(self, 'responseText', {value: JSON.stringify(j), writable: false});" +
            "          Object.defineProperty(self, 'response', {value: JSON.stringify(j), writable: false});" +
            "        } catch(e) {}" +
            "      }" +
            "      if (origOnReady) origOnReady.apply(self, arguments);" +
            "    };" +
            "  }" +
            "  return origSend.apply(this, arguments);" +
            "};" +

            // ============ AUTOPLAY NEXT ============
            "function setupAutoplay() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v || v._ytlAutoplay) return;" +
            "  v._ytlAutoplay = true;" +
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
            "setInterval(setupAutoplay, 2000);" +

            "})();";
    }
}
