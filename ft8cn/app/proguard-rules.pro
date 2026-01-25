# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep native methods and classes that use native libraries
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the native library loader class
-keep class com.bg7yoz.ft8cn.NativeLibrary { *; }

# Keep classes that use native methods
-keep class com.bg7yoz.ft8cn.ft8listener.** { *; }
-keep class com.bg7yoz.ft8cn.ft8transmit.** { *; }
-keep class com.bg7yoz.ft8cn.ft8signal.** { *; }
-keep class com.bg7yoz.ft8cn.wave.** { *; }
-keep class com.bg7yoz.ft8cn.ui.** { *; }