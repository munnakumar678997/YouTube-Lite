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
        String url = request.getUrl().toString();

        // Ad blocking
        if (isAdUrl(url)) {
            Log.d(TAG, "Blocked ad URL: " + url);
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        }

        // CSP stripping (simplified - a full implementation would modify headers)
        // This is a placeholder for more complex CSP manipulation if needed.
        // For now, we\'ll focus on blocking ad requests.

        return super.shouldInterceptRequest(view, request);
    }

    private boolean isAdUrl(String url) {
        return url.contains("googleads") ||
                url.contains("doubleclick") ||
                url.contains("ad_break") ||
                url.contains("pagead") ||
                url.contains("adservice.google.com") ||
                url.contains("youtube.com/api/ads") ||
                url.contains("googlesyndication.com") ||
                url.contains("adsystem.com") ||
                url.contains("ad.youtube.com") ||
                url.contains("doubleclick.net") ||
                url.contains("googleadservices.com") ||
                url.contains("ad.googlesyndication.com") ||
                url.contains("policybazaar.com") ||
                url.contains("insurancedekho.com");
    }

    // Method to inject JavaScript after page load (for ad object stripping and MediaSession polyfill)
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        // Inject CSS to hide ads
        view.evaluateJavascript(getCssInjectionScript(), null);
        // Inject ad-blocking JavaScript (fetch/XHR and MutationObserver)
        view.evaluateJavascript(getAdBlockingScript(), null);
        // Inject background playback JavaScript
        view.evaluateJavascript(getBackgroundPlaybackScript(), null);
        view.evaluateJavascript(getAutoplayScript(), null);
    }

    private String getCssInjectionScript() {
        return "(function() {" +
                "var style = document.createElement(\'style\');" +
                "style.innerHTML = `" +
                "  ytd-ad-slot-renderer, " +
                "  ytd-promoted-sparkles-web-renderer, " +
                "  ytd-promoted-video-renderer, " +
                "  ytd-display-ad-renderer, " +
                "  ytd-statement-banner-renderer, " +
                "  ytd-in-feed-ad-layout-renderer, " +
                "  ytd-banner-promo-renderer, " +
                "  ytm-promoted-sparkles-web-renderer, " +
                "  ytm-promoted-video-renderer, " +
                "  ytm-statement-banner-renderer, " +
                "  ytm-in-feed-ad-layout-renderer, " +
                "  .ad-container, " +
                "  .ad-showing, " +
                "  .ad-display, " +
                "  .ytp-ad-module, " +
                "  .ytp-ad-image-overlay, " +
                "  .video-ads, " +
                "  .ytp-ad-progress-list, " +
                "  #masthead-ad, " +
                "  #player-ads, " +
                "  #offers, " +
                "  ad-slot-renderer, " +
                "  ytm-companion-ad-renderer " +
                "  { display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; height: 0 !important; width: 0 !important; }`;" +
                "document.head.appendChild(style);" +
                "})();";
    }

    private String getAdBlockingScript() {
        // This script will intercept fetch/XHR and strip ad objects from YouTube API JSON responses
        // This is a simplified version, a full implementation would be more robust.
        return "" +
                "(function() {" +
                "    var originalFetch = window.fetch;" +
                "    window.fetch = function() {" +
                "        var url = arguments[0].toString();" +
                "        if (url.includes(\'googleads\') || url.includes(\'doubleclick\') || url.includes(\'ad_break\') || url.includes(\'pagead\') || url.includes(\'adservice.google.com\') || url.includes(\'youtube.com/api/ads\') || url.includes(\'googlesyndication.com\') || url.includes(\'adsystem.com\') || url.includes(\'ad.youtube.com\') || url.includes(\'doubleclick.net\') || url.includes(\'googleadservices.com\') || url.includes(\'ad.googlesyndication.com\') || url.includes(\'policybazaar.com\') || url.includes(\'insurancedekho.com\')) {" +
                "            console.log(\'Blocked fetch request: \' + url);" +
                "            return Promise.resolve(new Response(\'\'));" +
                "        }" +
                "        return originalFetch.apply(this, arguments).then(response => {" +
                "            if (url.includes(\'youtubei/v1\')) {" +
                "                return response.clone().json().then(json => {" +
                "                    if (json.adSlots) delete json.adSlots;" +
                "                    if (json.playerAds) delete json.playerAds;" +
                "                    if (json.adPlacements) delete json.adPlacements;" +
                "                    if (json.adBreakHeartbeatParams) delete json.adBreakHeartbeatParams;" +
                "                    if (json.playerResponse && json.playerResponse.adPlacements) delete json.playerResponse.adPlacements;" +
                "                    if (json.playerResponse && json.playerResponse.playerAds) delete json.playerResponse.playerAds;" +
                "                    if (json.playerResponse && json.playerResponse.adSlots) delete json.playerResponse.adSlots;" +
                "                    if (json.playerResponse && json.playerResponse.playbackTracking && json.playerResponse.playbackTracking.videostatsPlaybackUrl) delete json.playerResponse.playbackTracking.videostatsPlaybackUrl;" +
                "                    if (json.playerResponse && json.playerResponse.playbackTracking && json.playerResponse.playbackTracking.pTrackingUrl) delete json.playerResponse.playbackTracking.pTrackingUrl;" +
                "                    if (json.playerResponse && json.playerResponse.adSafetyReason) delete json.playerResponse.adSafetyReason;" +
                "                    if (json.playerResponse && json.playerResponse.playerConfig && json.playerResponse.playerConfig.ads) delete json.playerResponse.playerConfig.ads;" +
                "                    return new Response(JSON.stringify(json), {headers: response.headers});" +
                "                }).catch(() => response);" +
                "            }" +
                "            return response;" +
                "        });" +
                "    };" +
                "    var originalXHRopen = XMLHttpRequest.prototype.open;" +
                "    XMLHttpRequest.prototype.open = function(method, url) {" +
                "        if (url.includes(\'googleads\') || url.includes(\'doubleclick\') || url.includes(\'ad_break\') || url.includes(\'pagead\') || url.includes(\'adservice.google.com\') || url.includes(\'youtube.com/api/ads\') || url.includes(\'googlesyndication.com\') || url.includes(\'adsystem.com\') || url.includes(\'ad.youtube.com\') || url.includes(\'doubleclick.net\') || url.includes(\'googleadservices.com\') || url.includes(\'ad.googlesyndication.com\') || url.includes(\'policybazaar.com\') || url.includes(\'insurancedekho.com\')) {" +
                "            console.log(\'Blocked XHR request: \' + url);" +
                "            this._blocked = true;" +
                "        }" +
                "        return originalXHRopen.apply(this, arguments);" +
                "    };" +
                "    var originalXHRsend = XMLHttpRequest.prototype.send;" +
                "    XMLHttpRequest.prototype.send = function() {" +
                "        if (this._blocked) {" +
                "            console.log(\'Prevented sending blocked XHR request.\');" +
                "            return;" +
                "        }" +
                "        return originalXHRsend.apply(this, arguments);" +
                "    };" +
                "    " +
                "    // MutationObserver to aggressively remove ad elements from DOM" +
                "    var observer = new MutationObserver(function(mutations) {" +
                "        mutations.forEach(function(mutation) {" +
                "            if (mutation.addedNodes) {" +
                "                mutation.addedNodes.forEach(function(node) {" +
                "                    if (node.nodeType === 1) {" + // Element node
                "                        var textContent = node.textContent || node.innerText || \'\';" +
                "                        var lowerText = textContent.toLowerCase();" +
                "                        if (lowerText.includes(\'sponsored\') || lowerText.includes(\'visit advertiser\') || lowerText.includes(\'ad •\')) {" +
                "                            // Check if it\'s likely an ad container (not just a comment mentioning \'sponsored\')" +
                "                            if (node.tagName.includes(\'YTD-\\\') || node.tagName.includes(\'YTM-\\\') || node.classList.contains(\'ad-container\') || node.querySelector(\".ad-container\") || node.querySelector(\"[aria-label=\\\"Sponsored\\\"]\") || node.querySelector(\'ytm-badge[class*=\\\"ad\\\"]\')) {" +
                "                                console.log(\'Removed ad element via MutationObserver\');" +
                "                                node.remove();" +
                "                            }" +
                "                        }" +
                "                        // Remove specific ad tags" +
                "                        var adTags = [\'ytd-ad-slot-renderer\', \'ytd-promoted-sparkles-web-renderer\', \'ytd-promoted-video-renderer\', \'ytd-display-ad-renderer\', \'ytd-in-feed-ad-layout-renderer\', \'ytm-promoted-sparkles-web-renderer\', \'ytm-promoted-video-renderer\', \'ytm-in-feed-ad-layout-renderer\', \'ytm-companion-ad-renderer\'];" +
                "                        if (adTags.includes(node.tagName.toLowerCase())) {" +
                "                            console.log(\'Removed specific ad tag: \' + node.tagName);" +
                "                            node.remove();" +
                "                        }" +
                "                        // Search within the added node for ad elements" +
                "                        adTags.forEach(function(tag) {" +
                "                            var elements = node.querySelectorAll(tag);" +
                "                            elements.forEach(function(el) { el.remove(); });" +
                "                        });" +
                "                        // Remove elements with \'Sponsored\' badge" +
                "                        var sponsoredBadges = node.querySelectorAll(\'ytm-badge, ytd-badge-supported-renderer\');" +
                "                        sponsoredBadges.forEach(function(badge) {" +
                "                            if (badge.textContent.toLowerCase().includes(\'sponsored\') || badge.textContent.toLowerCase().includes(\'ad\')) {" +
                "                                var container = badge.closest(\'ytm-item-section-renderer, ytd-rich-item-renderer, ytm-rich-item-renderer\');" +
                "                                if (container) {" +
                "                                    console.log(\'Removed sponsored container\');" +
                "                                    container.remove();" +
                "                                }" +
                "                            }" +
                "                        });" +
                "                    }" +
                "                });" +
                "            }" +
                "        });" +
                "    });" +
                "    observer.observe(document.documentElement, { childList: true, subtree: true });" +
                "})();";
    }

    private String getAutoplayScript() {
        return "" +
                "(function() {" +
                "    function setupAutoplay() {" +
                "        var video = document.querySelector(\'video\');" +
                "        if (video) {" +
                "            video.addEventListener(\'ended\', function() {" +
                "                console.log(\'Video ended, attempting to play next.\');" +
                "                var nextButton = document.querySelector(\".ytp-next-button\") || document.querySelector(\".ytm-next-button\");" +
                "                if (nextButton) {" +
                "                    nextButton.click();" +
                "                    console.log(\'Clicked next button.\');" +
                "                } else {" +
                "                    console.log(\'Next button not found, trying to find related video.\');" +
                "                    // Fallback for when there's no explicit next button (e.g., on mobile YouTube)" +
                "                    var relatedVideo = document.querySelector(\'ytd-compact-video-renderer a#thumbnail, ytm-compact-video-renderer a#thumbnail\');" +
                "                    if (relatedVideo) {" +
                "                        relatedVideo.click();" +
                "                        console.log(\'Clicked related video.\');" +
                "                    } else {" +
                "                        console.log(\'No next video or related video found.\');" +
                "                    }" +
                "                }" +
                "            });" +
                "            console.log(\'Autoplay event listener added to video.\');" +
                "        } else {" +
                "            console.log(\'Video element not found, retrying in 1 second.\');" +
                "            setTimeout(setupAutoplay, 1000);" +
                "        }" +
                "    }" +
                "    setupAutoplay();" +
                "})();";
    }

    private String getBackgroundPlaybackScript() {
        // This script will polyfill MediaSession API and communicate with Android via JS bridge
        return "" +
                "(function() {" +
                "    if (\'mediaSession\' in navigator) {" +
                "        navigator.mediaSession.setActionHandler(\'play\', function() { Android.play(); });" +
                "        navigator.mediaSession.setActionHandler(\'pause\', function() { Android.pause(); });" +
                "        navigator.mediaSession.setActionHandler(\'nexttrack\', function() { Android.nextTrack(); });" +
                "        navigator.mediaSession.setActionHandler(\'previoustrack\', function() { Android.previousTrack(); });" +
                "    }" +
                "})();";
    }
}
