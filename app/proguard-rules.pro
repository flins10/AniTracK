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
}

# Hide original source file names and line mappings
-renamesourcefileattribute ""

# Protect Android architecture components and Room DAOs/Entities
-keep class com.example.MainActivity { *; }
-keep class com.example.worker.** { *; }
-keep class com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Protect Moshi and serialization data structures
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.example.data.remote.MalUserProfile { *; }
-keep class com.example.data.remote.MalSyncLog { *; }
-keep class com.example.data.remote.AiEpisodeInfo { *; }

# Network and reflection safety under aggressive obfuscation
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn kotlinx.coroutines.**
