# Aggressive Code Obfuscation & Security Hardening
-repackageclasses ''
-allowaccessmodification
-flattenpackagehierarchy ''

# Strip logging statements from compiled DEX
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Hide original source file names and line mappings
-renamesourcefileattribute ""

# Protect Android architecture components and Room DAOs/Entities
-keep class com.example.MainActivity { *; }
-keep class com.example.AnimeApplication { *; }
-keep class com.example.worker.** { *; }
-keep class com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Protect Moshi, Gson, and serialization data structures
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.google.gson.annotations.SerializedName *;
}
-keep class com.example.data.remote.** { *; }
-keep class com.example.model.** { *; }

# Keep obfuscated vault runtime entry points minimized but callable
-keepclassmembers class com.example.config.VaultMaterial {
    java.lang.String decodeEndpoint();
    java.lang.String decodeCredential();
}

# Network and reflection safety under aggressive obfuscation
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
