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

    private String getAdBlockScript() {
        return "(" + "function() {" +
            "if (window._ytlAdBlockSetup) return;" +
            "window._ytlAdBlockSetup = true;" +

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

            // AGGRESSIVE AD SKIP - This is the key fix for black screen
            // YouTube now injects ads server-side into the video stream
            // We detect ad state and immediately skip past it
            "function skipAds() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) return;" +

            // Method 1: Check for .ad-showing class on player
            "  var player = document.querySelector('.html5-video-player');" +
            "  var adShowing = player && player.classList.contains('ad-showing');" +

            // Method 2: Check for ad overlay elements
            "  if (!adShowing) {" +
            "    adShowing = !!document.querySelector('.ytp-ad-player-overlay, .ytp-ad-player-overlay-instream-info, .ytp-ad-text, .ytp-ad-preview-container, .ytp-ad-skip-button-slot');" +
            "  }" +

            // Method 3: Check if video duration is suspiciously short (ad duration)
            // and there's no proper title loaded yet
            "  if (adShowing) {" +
            "    v.currentTime = 9999;" +
            "    v.playbackRate = 16;" +
            "  } else {" +
            "    if (v.playbackRate === 16) v.playbackRate = 1;" +
            "  }" +

            // Click any skip button
            "  var skipBtns = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, [class*=\"skip-button\"], .ytp-ad-skip-button-container button');" +
            "  skipBtns.forEach(function(b) { b.click(); });" +

            // Remove ad overlay elements
            "  var adEls = document.querySelectorAll('.ytp-ad-module, .video-ads, .ytp-ad-overlay-container, .ytp-ad-player-overlay');" +
            "  adEls.forEach(function(el) { el.remove(); });" +
            "}" +

            // Run ad skip very frequently
            "setInterval(skipAds, 50);" +

            // MutationObserver to catch ads immediately when they appear
            "var adMutObs = new MutationObserver(function(mutations) {" +
            "  for (var i = 0; i < mutations.length; i++) {" +
            "    var m = mutations[i];" +
            "    if (m.type === 'attributes' && m.attributeName === 'class') {" +
            "      var t = m.target;" +
            "      if (t.classList && t.classList.contains('ad-showing')) {" +
            "        skipAds();" +
            "      }" +
            "    }" +
            "  }" +
            "});" +
            "var playerEl = document.querySelector('.html5-video-player');" +
            "if (playerEl) {" +
            "  adMutObs.observe(playerEl, {attributes: true, attributeFilter: ['class']});" +
            "} else {" +
            "  setTimeout(function() {" +
            "    var p = document.querySelector('.html5-video-player');" +
            "    if (p) adMutObs.observe(p, {attributes: true, attributeFilter: ['class']});" +
            "  }, 2000);" +
            "}" +

            // MutationObserver to remove feed ads
            "var feedObserver = new MutationObserver(function(mutations) {" +
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
            "    });" +
            "  });" +
            "});" +
            "feedObserver.observe(document.documentElement, {childList: true, subtree: true});" +

            // Remove existing feed ads
            "function removeExistingAds() {" +
            "  var selectors = ['ytm-promoted-sparkles-web-renderer','ytd-ad-slot-renderer','ytd-in-feed-ad-layout-renderer','ytm-companion-ad-renderer','ytd-carousel-ad-renderer','ytd-promoted-video-renderer','ytd-display-ad-renderer','.ad-container','.ytp-ad-module','.video-ads','#masthead-ad','#player-ads'];" +
            "  selectors.forEach(function(s) {" +
            "    document.querySelectorAll(s).forEach(function(el) { el.remove(); });" +
            "  });" +
            "}" +
            "removeExistingAds();" +
            "setInterval(removeExistingAds, 2000);" +

            // Intercept fetch to strip ad data from API responses
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
            "          if (j.adPlacements) delete j.adPlacements;" +
            "          if (j.playerAds) delete j.playerAds;" +
            "          return new Response(JSON.stringify(j), {status: resp.status, headers: resp.headers});" +
            "        } catch(e) { return resp; }" +
            "      });" +
            "    }" +
            "    return resp;" +
            "  });" +
            "};" +

            // Intercept XHR for player requests to strip ads
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
            "    var origOnLoad = self.onload;" +
            "    self.onload = function() {" +
            "      try {" +
            "        var j = JSON.parse(self.responseText);" +
            "        delete j.adSlots; delete j.playerAds; delete j.adPlacements;" +
            "        delete j.adBreakHeartbeatParams; delete j.adBreakParams;" +
            "        Object.defineProperty(self, 'responseText', {value: JSON.stringify(j)});" +
            "      } catch(e) {}" +
            "      if (origOnLoad) origOnLoad.apply(self, arguments);" +
            "    };" +
            "  }" +
            "  return origSend.apply(this, arguments);" +
            "};" +

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
