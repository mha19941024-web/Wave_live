# Wave Live
# Release build: keep Compose and application classes safe.

-keep class com.vyro.app.** { *; }

-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

-dontwarn okhttp3.**
-dontwarn okio.**

-keepattributes *Annotation*
-keepattributes Signature
