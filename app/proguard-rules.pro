# ProGuard rules for Shopping List App

# Keep all models and API classes (Moshi serialization)
-keep class com.example.shoppinglist.shared.** { *; }
-keep class com.example.shoppinglist.** { *; }

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

# Keep Android Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Keep Glance
-keep class androidx.glance.** { *; }
-keep interface androidx.glance.** { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }
-keep interface androidx.datastore.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
