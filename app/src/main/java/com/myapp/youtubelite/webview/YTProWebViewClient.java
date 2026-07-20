package com.myapp.youtubelite.webview;

    import android.content.Context;
    import android.util.Log;
    import android.webkit.WebResourceRequest;
    import android.webkit.WebResourceResponse;
    import android.webkit.WebView;
    import android.webkit.WebViewClient;

    import java.io.ByteArrayInputStream;

    public class YTProWebViewClient extends WebViewClient {

      private static final String TAG = "YTProWebViewClient";
      private final Context context;

      public YTProWebViewClient(Context context) {
          this.context = context;
      }

      // ── Network-level ad blocking ────────────────────────────────────────────
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view,
              WebResourceRequest request) {
          String url = request.getUrl().toString().toLowerCase();
          if (isAdUrl(url)) {
              Log.d(TAG, "Blocked ad URL: " + url);
              return new WebResourceResponse("text/plain", "utf-8",
                      new ByteArrayInputStream("".getBytes()));
          }
          return super.shouldInterceptRequest(view, request);
      }

      private boolean isAdUrl(String url) {
          // ── Whitelist: never block YouTube content or essential resources ──
          if (url.contains("googlevideo.com")       ||
              url.contains("videoplayback")          ||
              url.contains("youtubei/v1/player")     ||
              url.contains("youtubei/v1/next")       ||
              url.contains("youtubei/v1/browse")     ||
              url.contains("youtubei/v1/search")     ||
              url.contains("youtubei/v1/guide")      ||
              url.contains("youtube.com/watch")      ||
              url.contains("youtube.com/results")    ||
              url.contains("youtube.com/shorts")     ||
              url.contains("yt3.ggpht.com")          ||
              url.contains("i.ytimg.com")            ||
              url.contains("youtube.com/s/")         ||
              url.contains("youtube.com/youtubei")   ||
              url.contains("youtube.com/embed")      ||
              url.contains("youtube.com/api/stats")  ||
              url.contains("youtube.com/ptracking")  ||
              url.contains("youtube.com/api/timedtext") ||
              url.contains("gstatic.com")) {
              return false;
          }
          // ── Blacklist: known ad-network domains ──
          return url.contains("doubleclick.net")          ||
                 url.contains("googlesyndication.com")    ||
                 url.contains("googleadservices.com")     ||
                 url.contains("adservice.google.com")     ||
                 url.contains("ad.youtube.com")           ||
                 url.contains("youtube.com/api/ads")      ||
                 url.contains("youtube.com/pagead")       ||
                 url.contains("youtube.com/get_midroll")  ||
                 url.contains("adsystem.com")             ||
                 url.contains("admob.com")                ||
                 url.contains("ads.google.com")           ||
                 url.contains("fundingchoicesmessages.google.com") ||
                 url.contains("tpc.googlesyndication.com")||
                 url.contains("securepubads")             ||
                 url.contains("imasdk.googleapis.com")    ||
                 url.contains("static.doubleclick.net")   ||
                 url.contains("s0.2mdn.net")              ||
                 url.contains("survey.g.doubleclick.net") ||
                 url.contains("google.com/adsense");
      }

      // ── Full-page load: inject the master script ─────────────────────────────
      @Override
      public void onPageFinished(WebView view, String url) {
          super.onPageFinished(view, url);
          view.evaluateJavascript(getMasterScript(), null);
      }

      // FIX BUG #9 (cleanup): Both branches already returned false, so the method
      // was dead code.  Simplified to a single return statement.
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
          return false; // Let the WebView handle all YouTube navigation internally
      }

      // FIX BUG #8: SPA navigation re-injection.
      // YouTube is a Single-Page App — onPageFinished does NOT fire when the user
      // taps a video from the home feed.  doUpdateVisitedHistory fires on every
      // pushState/replaceState call, making it the correct hook for SPA navigation.
      // Previously this method only set a JS flag; now it actually re-injects the
      // per-navigation script so ad-skip and autoplay work on every new video.
      @Override
      public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
          super.doUpdateVisitedHistory(view, url, isReload);
          if (url.contains("youtube.com/watch") || url.contains("youtube.com/shorts")) {
              final WebView v = view;
              // Small delay lets YouTube's SPA finish mounting the new player DOM
              view.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                      v.evaluateJavascript(getNavScript(), null);
                  }
              }, 700);
          }
      }

      // ── Master script (runs once per full page load) ─────────────────────────
      private String getMasterScript() {
          return "(function() {" +
              "if (window._ytlMasterV5) return;" +
              "window._ytlMasterV5 = true;" +

              // ── CSS: hide ad elements ──
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
                  ".ad-container,.ytp-ad-module,.ytp-ad-overlay-container," +
                  ".ytp-ad-image-overlay,.ytp-ad-text-overlay,.video-ads," +
                  "#masthead-ad,#player-ads,#ad-slot," +
                  "[class*=\"ad-badge\"],[class*=\"badge-style-type-ad\"]" +
                  "{ display:none!important;height:0!important;overflow:hidden!important; }';" +
              "document.head.appendChild(css);" +

              // ── Background playback: fake document visibility ──
              "Object.defineProperty(document,'hidden',{get:function(){return false;},configurable:true});" +
              "Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';},configurable:true});" +
              "document.addEventListener('visibilitychange',function(e){e.stopImmediatePropagation();},true);" +
              "window._ytlUserPaused = false;" +
              "var origPause = HTMLVideoElement.prototype.pause;" +
              "HTMLVideoElement.prototype.pause = function() {" +
              "  if (window._ytlUserPaused) {" +
              "    window._ytlUserPaused = false;" +
              "    return origPause.apply(this, arguments);" +
              "  }" +
              "  return undefined;" +
              "};" +

              // ── Ad-skip helpers (also used by nav script & observer) ──
              "window._ytlIsAdPlaying = function() {" +
              "  var p = document.querySelector('.html5-video-player');" +
              "  if (p && p.classList.contains('ad-showing')) return true;" +
              "  if (document.querySelector('.ytp-ad-player-overlay,.ytp-ad-skip-button-slot," +
              "      .ytp-ad-preview-container,.ytp-ad-message-container')) return true;" +
              "  if (document.querySelector('[class*=\"ad-interrupting\"],[class*=\"ad-created\"]')) return true;" +
              "  return false;" +
              "};" +
              "window._ytlSkipAd = function() {" +
              "  var v = document.querySelector('video');" +
              "  if (!v || !window._ytlIsAdPlaying()) return;" +
              "  v.currentTime = v.duration ? v.duration - 0.1 : 9999;" +
              "  v.playbackRate = 16;" +
              "  var btn = document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,button[class*=\"skip\"]');" +
              "  if (btn) setTimeout(function(){ btn.click(); }, 100);" +
              "};" +

              // ── XHR hook: strip ad fields from YouTube API responses ──
              "var _ytlOrigOpen = XMLHttpRequest.prototype.open;" +
              "XMLHttpRequest.prototype.open = function(method, url) {" +
              "  this._ytlUrl = url;" +
              "  return _ytlOrigOpen.apply(this, arguments);" +
              "};" +
              "var _ytlOrigSend = XMLHttpRequest.prototype.send;" +
              "XMLHttpRequest.prototype.send = function() {" +
              "  if (this._ytlUrl && (" +
              "      this._ytlUrl.indexOf('youtubei/v1/player') !== -1 ||" +
              "      this._ytlUrl.indexOf('youtubei/v1/next') !== -1)) {" +
              "    var self = this; var orig = this.onreadystatechange;" +
              "    this.onreadystatechange = function() {" +
              "      if (self.readyState === 4) {" +
              "        try {" +
              "          var j = JSON.parse(self.responseText);" +
              "          if (j) { delete j.adSlots; delete j.playerAds;" +
              "                   delete j.adPlacements; delete j.adBreakHeartbeatParams; }" +
              "        } catch(e) {}" +
              "      }" +
              "      if (orig) orig.apply(self, arguments);" +
              "    };" +
              "  }" +
              "  return _ytlOrigSend.apply(this, arguments);" +
              "};" +

              // ── Strip ytInitialPlayerResponse ad fields ──
              "try {" +
              "  if (window.ytInitialPlayerResponse) {" +
              "    delete window.ytInitialPlayerResponse.adSlots;" +
              "    delete window.ytInitialPlayerResponse.playerAds;" +
              "    delete window.ytInitialPlayerResponse.adPlacements;" +
              "    delete window.ytInitialPlayerResponse.adBreakHeartbeatParams;" +
              "  }" +
              "} catch(e) {}" +

              // ── Autoplay-next helper ──
              "window._ytlSetupAutoplay = function() {" +
              "  var v = document.querySelector('video');" +
              "  if (!v || v._ytlAutoplay) return;" +
              "  v._ytlAutoplay = true;" +
              "  v.setAttribute('playsinline','');" +
              "  v.setAttribute('webkit-playsinline','');" +
              "  v.addEventListener('ended', function() {" +
              "    var next = document.querySelector('.ytp-next-button,.ytm-autonav-bar a," +
              "        a.ytm-next-button,[class*=\"next-button\"]');" +
              "    if (next) { next.click(); return; }" +
              "    var rel = document.querySelector('ytm-compact-video-renderer a,ytd-compact-video-renderer a');" +
              "    if (rel) rel.click();" +
              "  });" +
              "};" +
              "window._ytlSetupAutoplay();" +

              // ── FIX BUG #9: MutationObserver instead of setInterval ──
              // setInterval ran every 2 seconds forever, wasting CPU/battery.
              // MutationObserver fires ONLY when the DOM actually changes, which is
              // both more efficient and more reliable for YouTube's SPA structure.
              "var _ytlObs = new MutationObserver(function(mutations) {" +
              "  var needSkip = false, needSetup = false;" +
              "  for (var i = 0; i < mutations.length; i++) {" +
              "    var m = mutations[i];" +
              "    if (m.type === 'attributes' && m.attributeName === 'class' &&" +
              "        m.target.classList && m.target.classList.contains('ad-showing')) {" +
              "      needSkip = true;" +
              "    }" +
              "    if (m.type === 'childList' && m.addedNodes.length > 0) {" +
              "      needSetup = true;" +
              "    }" +
              "  }" +
              "  if (needSkip)  window._ytlSkipAd();" +
              "  if (needSetup) window._ytlSetupAutoplay();" +
              "});" +
              "_ytlObs.observe(document.documentElement," +
              "  {childList:true, subtree:true, attributes:true, attributeFilter:['class']});" +
              "})();";
      }

      // ── Nav script (runs on every SPA video navigation) ──────────────────────
      // FIX BUG #8: A lightweight script that re-applies per-video setup without
      // repeating the one-time infrastructure already set up by getMasterScript().
      private String getNavScript() {
          return "(function() {" +
              // Reset the per-video autoplay flag so the new video element gets set up
              "var v = document.querySelector('video');" +
              "if (v) { v._ytlAutoplay = false; }" +
              // Run autoplay setup and ad-skip immediately for the new video
              "if (window._ytlSetupAutoplay) window._ytlSetupAutoplay();" +
              "if (window._ytlSkipAd)       window._ytlSkipAd();" +
              // Strip ad fields from the new video's player response if already available
              "try {" +
              "  if (window.ytInitialPlayerResponse) {" +
              "    delete window.ytInitialPlayerResponse.adSlots;" +
              "    delete window.ytInitialPlayerResponse.playerAds;" +
              "    delete window.ytInitialPlayerResponse.adPlacements;" +
              "    delete window.ytInitialPlayerResponse.adBreakHeartbeatParams;" +
              "  }" +
              "} catch(e) {}" +
              "})();";
      }
    }
    