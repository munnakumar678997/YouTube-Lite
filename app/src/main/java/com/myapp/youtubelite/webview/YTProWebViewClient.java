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
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        }
        return super.shouldInterceptRequest(view, request);
    }
    private boolean isAdUrl(String url) {
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
            return false;
        }
        return false;
    }
    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        super.doUpdateVisitedHistory(view, url, isReload);
        // This fires on EVERY URL change including SPA pushState/replaceState
        // This is the KEY hook for detecting SPA navigation on m.youtube.com
        if (url.contains("youtube.com/watch")) {
            view.evaluateJavascript("(function(){window._ytlNavDetected=Date.now();window._ytlNavHandled=false;})();", null);
        }
    }
    private String getMasterScript() {
        return "(function() {" +
            "if (window._ytlMasterV5) return;" +
            "window._ytlMasterV5 = true;" +

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
            "setInterval(skipAd, 100);" +

            // ============ CORE FIX: VIDEO RELOAD ON SPA NAVIGATION ============
            // The KEY insight from research:
            // - On first load, page loads fresh so no ad-waiting state
            // - On SPA navigation (2nd video click), YouTube's player enters ad-waiting state
            // - play() doesn't work because player state machine is in "ad" state
            // - loadVideoById() RESETS the player state machine, bypassing ad state
            // - If loadVideoById not available (mobile), force a page reload (which also bypasses ads)
            "var _ytlLastUrl = location.href;" +
            "var _ytlNavTime = 0;" +
            "var _ytlIsPlaying = true;" + // Assume first video is playing
            "var _ytlReloadDone = false;" +

            "function getVideoId() {" +
            "  try {" +
            "    var params = new URLSearchParams(window.location.search);" +
            "    return params.get('v');" +
            "  } catch(e) { return null; }" +
            "}" +

            "function isVideoPlaying() {" +
            "  var v = document.querySelector('video');" +
            "  if (!v) return false;" +
            "  return !v.paused && v.currentTime > 0.3 && v.readyState >= 3;" +
            "}" +

            "function forceLoadVideo() {" +
            "  if (_ytlReloadDone || _ytlIsPlaying) return;" +
            "  var videoId = getVideoId();" +
            "  if (!videoId) return;" +
            "  var elapsed = Date.now() - _ytlNavTime;" +
            "  if (elapsed < 1500) return;" + // Wait 1.5s before intervening

            // Check again if video started playing in the meantime
            "  if (isVideoPlaying()) { _ytlIsPlaying = true; return; }" +

            // APPROACH 1: Use movie_player.loadVideoById() (desktop YouTube internal API)
            "  var mp = document.getElementById('movie_player');" +
            "  if (!mp) mp = document.querySelector('.html5-video-player');" +
            "  if (mp && typeof mp.loadVideoById === 'function') {" +
            "    try {" +
            "      mp.loadVideoById(videoId, 0);" +
            "      _ytlReloadDone = true;" +
            "      return;" +
            "    } catch(e) {}" +
            "  }" +

            // APPROACH 2: Use movie_player.loadVideoByPlayerVars() if available
            "  if (mp && typeof mp.loadVideoByPlayerVars === 'function') {" +
            "    try {" +
            "      mp.loadVideoByPlayerVars({video_id: videoId});" +
            "      _ytlReloadDone = true;" +
            "      return;" +
            "    } catch(e) {}" +
            "  }" +

            // APPROACH 3: Try to navigate to the video URL directly (forces fresh load without full reload)
            "  if (elapsed > 1800) {" +
            "    _ytlReloadDone = true;" +
            // Navigate to the same URL which forces YouTube to do a fresh page load
            "    window.location.href = 'https://m.youtube.com/watch?v=' + videoId;" +
            "    return;" +
            "  }" +

            // APPROACH 4: Force page reload as last resort
            "  if (elapsed > 2500) {" +
            "    _ytlReloadDone = true;" +
            "    window.location.reload();" +
            "  }" +
            "}" +

            // Monitor URL changes for SPA navigation
            "setInterval(function() {" +
            "  var currentUrl = location.href;" +
            "  if (currentUrl !== _ytlLastUrl) {" +
            "    _ytlLastUrl = currentUrl;" +
            "    _ytlNavTime = Date.now();" +
            "    _ytlIsPlaying = false;" +
            "    _ytlReloadDone = false;" +
            "  }" +
            // Check if video is playing
            "  if (!_ytlIsPlaying && isVideoPlaying()) {" +
            "    _ytlIsPlaying = true;" +
            "  }" +
            // If not playing and nav happened, try to force
            "  if (!_ytlIsPlaying && !_ytlReloadDone && _ytlNavTime > 0) {" +
            "    forceLoadVideo();" +
            "  }" +
            "}, 500);" +

            // Also handle doUpdateVisitedHistory signal
            "setInterval(function() {" +
            "  if (window._ytlNavDetected && !window._ytlNavHandled) {" +
            "    var elapsed = Date.now() - window._ytlNavDetected;" +
            "    if (elapsed > 1500 && !isVideoPlaying() && !_ytlReloadDone) {" +
            "      window._ytlNavHandled = true;" +
            "      _ytlReloadDone = true;" +
            "      var videoId = getVideoId();" +
            "      var mp = document.getElementById('movie_player');" +
            "      if (!mp) mp = document.querySelector('.html5-video-player');" +
            "      if (mp && typeof mp.loadVideoById === 'function') {" +
            "        try { mp.loadVideoById(videoId, 0); return; } catch(e) {}" +
            "      }" +
            "      window.location.reload();" +
            "    } else if (isVideoPlaying()) {" +
            "      window._ytlNavHandled = true;" +
            "    }" +
            "  }" +
            "}, 500);" +

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
            "          if (j.playerConfig && j.playerConfig.adRequestConfig) delete j.playerConfig.adRequestConfig;" +
            "          if (j.adParams) delete j.adParams;" +
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
            "          if (j.playerConfig && j.playerConfig.adRequestConfig) delete j.playerConfig.adRequestConfig;" +
            "          if (j.adParams) delete j.adParams;" +
            "          Object.defineProperty(self, 'responseText', {value: JSON.stringify(j), writable: false});" +
            "          Object.defineProperty(self, 'response', {value: JSON.stringify(j), writable: false});" +
            "        } catch(e) {}" +
            "      }" +
            "      if (origOnReady) origOnReady.apply(self, arguments);" +
            "    };" +
            "  }" +
            "  return origSend.apply(this, arguments);" +
            "};" +

            // ============ STRIP ytInitialPlayerResponse ============
            "try {" +
            "  if (window.ytInitialPlayerResponse) {" +
            "    delete window.ytInitialPlayerResponse.adSlots;" +
            "    delete window.ytInitialPlayerResponse.playerAds;" +
            "    delete window.ytInitialPlayerResponse.adPlacements;" +
            "    delete window.ytInitialPlayerResponse.adBreakHeartbeatParams;" +
            "  }" +
            "} catch(e) {}" +

            // ============ MUTATION OBSERVER ============
            "var obs = new MutationObserver(function(mutations) {" +
            "  for (var i = 0; i < mutations.length; i++) {" +
            "    var m = mutations[i];" +
            "    if (m.type === 'attributes' && m.attributeName === 'class') {" +
            "      if (m.target.classList && m.target.classList.contains('ad-showing')) {" +
            "        skipAd();" +
            "      }" +
            "    }" +
            "  }" +
            "});" +
            "obs.observe(document.documentElement, {childList: true, subtree: true, attributes: true, attributeFilter: ['class']});" +

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
