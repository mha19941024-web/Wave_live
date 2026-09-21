# Wave Live - ProGuard / R8 rules
# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Keep application models and API data
-keep class com.vyro.app.** { *; }

# Keep Retrofit/JSON-style model fields if added later
-keepattributes *Annotation*
-keepattributes Signature
