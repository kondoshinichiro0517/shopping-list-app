# ProGuard rules for WearOS Shopping List App

# Keep all models and API classes (Moshi serialization)
-keep class com.example.shoppinglist.shared.** { *; }
-keep class com.example.shoppinglist.wear.** { *; }

# Keep Moshi and JSON serialization
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.Json class * { *; }
-keepclassmembers class * {
  @com.squareup.moshi.* <fields>;
}

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Kotlin
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-dontwarn kotlin.**

# Keep AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep Wear
-keep class androidx.wear.** { *; }
-keep interface androidx.wear.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
