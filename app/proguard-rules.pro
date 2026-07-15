# Add project specific ProGuard rules here.
# You can control the default rules already applied by Gradle using the
# proguardFiles setting in the build.gradle file.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name of your WebView subclass:
#-keepclassmembers class fqcn.of.your.webview.WebviewSubclass {
#    <methods>;
#}

-keepattributes JavascriptInterface
-keep class com.myapp.youtubelite.webview.WebAppInterface {
    <methods>;
}

-dontwarn org.chromium.support.**
-dontwarn android.webkit.**

-keep class androidx.webkit.** { *; }
-keep class org.chromium.support_lib_glue.** { *; }
-keep class org.chromium.android_webview.** { *; }

# Keep all classes that extend from WebViewClient and WebChromeClient
-keep class * extends android.webkit.WebViewClient { *; }
-keep class * extends android.webkit.WebChromeClient { *; }

# Keep all classes that extend from Service
-keep class * extends android.app.Service { *; }

# Keep all classes that extend from BroadcastReceiver
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep all classes that extend from Activity
-keep class * extends android.app.Activity { *; }

# For MediaSession and MediaNotification
-keep class android.support.v4.media.session.MediaSessionCompat$Callback { *; }
-keep class android.support.v4.media.MediaMetadataCompat { *; }
-keep class android.support.v4.media.session.PlaybackStateCompat { *; }
-keep class android.support.v4.media.session.MediaControllerCompat { *; }
-keep class android.support.v4.media.session.MediaSessionCompat { *; }

# For ForegroundService
-keep class androidx.core.app.NotificationCompat$Builder { *; }
-keep class androidx.core.app.NotificationCompat { *; }

# For WakeLock
-keep class android.os.PowerManager$WakeLock { *; }

# For WebView
-keep class android.webkit.WebView { *; }
-keep class android.webkit.WebSettings { *; }

# For other AndroidX dependencies
-keep class androidx.core.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class com.google.android.material.** { *; }
-keep class androidx.constraintlayout.** { *; }

# For Kotlin
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }

# For general Android classes
-keep class android.app.Application { *; }
-keep class android.content.Context { *; }
-keep class android.content.Intent { *; }
-keep class android.os.Bundle { *; }
-keep class android.view.View { *; }
-keep class android.view.ViewGroup { *; }
-keep class android.widget.** { *; }

# For resources
-keepclassmembers class **.R$* {
    public static <fields>;
}

# For annotations
-keepattributes *Annotation*
