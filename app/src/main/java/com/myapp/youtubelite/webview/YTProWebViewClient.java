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
                "  ytm-companion-ad-renderer, " +
                "  ytm-action-button-renderer:has(a[href*=\\"policybazaar\"]), " +
                "  ytm-action-button-renderer:has(a[href*=\\"insurancedekho\"]), " +
                "  ytm-item-section-renderer:has(ytm-badge:contains(\\"Sponsored\\")), " +
                "  ytm-item-section-renderer:has(ytm-badge:contains(\\"Ad\\")), " +
                "  ytd-carousel-ad-renderer, " +
                "  ytd-promoted-sparkles-text-renderer, " +
                "  ytd-compact-promoted-video-renderer, " +
                "  ytd-engagement-panel-section-list-renderer[target-id=\\"engagement-panel-ads\\"] " +
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
                "                    if (node.nodeType === 1) { // Element node\n                        var textContent = node.textContent || node.innerText || \'\';\n                        var lowerText = textContent.toLowerCase();\n                        if (lowerText.includes(\'sponsored\') || lowerText.includes(\'visit advertiser\') || lowerText.includes(\'ad •\')) {\n                            // Check if it\'s likely an ad container (not just a comment mentioning \'sponsored\')\n                            if (node.tagName.includes(\'YTD-\\') || node.tagName.includes(\'YTM-\\') || node.classList.contains(\'ad-container\') || node.querySelector(\".ad-container\") || node.querySelector(\"[aria-label=\\"Sponsored\\"]\") || node.querySelector(\'ytm-badge[class*=\\"ad\\"]\')) {\n                                console.log(\'Removed ad element via MutationObserver\');\n                                node.remove();\n                            }\n                        }\n                        // Remove specific ad tags\n                        var adTags = [\'ytd-ad-slot-renderer\', \'ytd-promoted-sparkles-web-renderer\', \'ytd-promoted-video-renderer\', \'ytd-display-ad-renderer\', \'ytd-in-feed-ad-layout-renderer\', \'ytm-promoted-sparkles-web-renderer\', \'ytm-promoted-video-renderer\', \'ytm-in-feed-ad-layout-renderer\', \'ytm-companion-ad-renderer\', \'ytd-carousel-ad-renderer\', \'ytd-promoted-sparkles-text-renderer\', \'ytd-compact-promoted-video-renderer\', \'ytd-engagement-panel-section-list-renderer[target-id=\\"engagement-panel-ads\\"]\'];\n                        if (adTags.includes(node.tagName.toLowerCase())) {\n                            console.log(\'Removed specific ad tag: \' + node.tagName);\n                            node.remove();\n                        }\n                        // Search within the added node for ad elements\n                        adTags.forEach(function(tag) {\n                            var elements = node.querySelectorAll(tag);\n                            elements.forEach(function(el) { el.remove(); });\n                        });\n                        // Remove elements with \'Sponsored\' badge\n                        var sponsoredBadges = node.querySelectorAll(\'ytm-badge, ytd-badge-supported-renderer, .badge-style-type-ad\');\n                        sponsoredBadges.forEach(function(badge) {\n                            if (badge.textContent.toLowerCase().includes(\'sponsored\') || badge.textContent.toLowerCase().includes(\'ad\')) {\n                                var container = badge.closest(\'ytm-item-section-renderer, ytd-rich-item-renderer, ytm-rich-item-renderer, ytm-video-with-context-renderer\');\n                                if (container) {\n                                    console.log(\'Removed sponsored container\');\n                                    container.remove();\n                                }\n                            }\n                        });\n                        \n                        // Also check the node itself if it\'s a container\n                        if (node.tagName && (node.tagName.toLowerCase() === \'ytm-item-section-renderer\' || node.tagName.toLowerCase() === \'ytd-rich-item-renderer\' || node.tagName.toLowerCase() === \'ytm-rich-item-renderer\' || node.tagName.toLowerCase() === \'ytm-video-with-context-renderer\')) {\n                            var badges = node.querySelectorAll(\'ytm-badge, ytd-badge-supported-renderer, .badge-style-type-ad\');\n                            badges.forEach(function(badge) {\n                                if (badge.textContent.toLowerCase().includes(\'sponsored\') || badge.textContent.toLowerCase().includes(\'ad\')) {\n                                    console.log(\'Removed sponsored container (self)\');\n                                    node.remove();\n                                }\n                            });\n                            \n                            // Check for \"Visit Advertiser\" text\n                            if (node.textContent.toLowerCase().includes(\'visit advertiser\')) {\n                                console.log(\'Removed visit advertiser container (self)\');\n                                node.remove();\n                            }\n                        }\n                    }\n                });\n            }\n        });\n    });\n    observer.observe(document.documentElement, { childList: true, subtree: true });" +
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
                "                    // Fallback for when there\'s no explicit next button (e.g., on mobile YouTube)" +
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
        return "" +
                "(function() {" +
                "    var video = document.querySelector(\'video\');" +
                "    if (video) {" +
                "        video.setAttribute(\'playsinline\', \'\');" +
                "        video.setAttribute(\'webkit-playsinline\', \'\');" +
                "        video.play(); // Attempt to play immediately to enable background playback
" +
                "        // Use Media Session API for better background control and notifications
" +
                "        if ('mediaSession' in navigator) {" +
                "            navigator.mediaSession.setActionHandler('play', function() { video.play(); });" +
                "            navigator.mediaSession.setActionHandler('pause', function() { video.pause(); });" +
                "            navigator.mediaSession.setActionHandler('previoustrack', function() { window.history.back(); });" +
                "            navigator.mediaSession.setActionHandler('nexttrack', function() { /* Implement next track logic if available */ });" +
                "            // Update metadata if available
" +
                "            var updateMediaSession = function() {" +
                "                if (document.title) {" +
                "                    navigator.mediaSession.metadata = new MediaMetadata({" +
                "                        title: document.title," +
                "                        artist: document.querySelector(\'#owner-name a\') ? document.querySelector(\'#owner-name a\').innerText : \'Unknown Artist\'," +
                "                        album: \'YouTube Lite\'," +
                "                        artwork: [" +
                "                            { src: \'https://www.youtube.com/s/desktop/e913708d/img/favicon_96x96.png\', sizes: \'96x96\', type: \'image/png\' }" +
                "                        ]" +
                "                    });" +
                "                }" +
                "            };
" +
                "            updateMediaSession();
" +
                "            // Observe changes in title for dynamic updates
" +
                "            var titleObserver = new MutationObserver(updateMediaSession);
" +
                "            titleObserver.observe(document.querySelector(\'title\'), { childList: true });" +
                "        }" +
                "        console.log(\'Background playback script injected and Media Session API configured.\');" +
                "    } else {" +
                "        console.log(\'Video element not found for background playback, retrying in 1 second.\');" +
                "        setTimeout(function() {\n            var videoRetry = document.querySelector(\'video\');\n            if (videoRetry) {\n                videoRetry.setAttribute(\'playsinline\', \'\');\n                videoRetry.setAttribute(\'webkit-playsinline\', \'\');\n                videoRetry.play();\n                console.log(\'Video element found and playsinline set on retry.\');\n            } else {\n                console.log(\'Video element still not found after retry.\');\n            }\n        }, 1000);" +
                "    }" +
                "})();";
    }
}
