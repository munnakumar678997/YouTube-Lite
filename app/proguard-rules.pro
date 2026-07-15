# ProGuard rules for YouTube Lite
-dontwarn javax.annotation.**
-dontwarn org.chromium.support.**

-keepclassmembers class com.myapp.youtubelite.webview.WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}
