# Add project specific ProGuard rules here.

# Keep Room entities and DAOs
-keep class com.personaltrainer.data.entities.** { *; }
-keep class com.personaltrainer.data.dao.** { *; }

# Keep Hilt generated components
-keep class hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.** { *; }

# Keep Gson serialization/deserialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }

# Keep enum names (used in TypeConverters)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
