# Strip debug/verbose logging statements from release DEX
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Protect Android architecture components, ViewModels, WorkManager, Startup, and Room DAOs/Entities
-keep class com.example.MainActivity { *; }
-keep class com.example.AnimeApplication { *; }
-keep class com.example.viewmodel.** { *; }
-keep class com.example.worker.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.config.VaultMaterial { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class androidx.work.** { *; }
-keep class androidx.startup.** { *; }
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Protect Moshi (including KSP-generated *JsonAdapter), Gson, Retrofit, and data models
-keep class *JsonAdapter { *; }
-keepclasseswithmembers class * {
    public <init>(com.squareup.moshi.Moshi);
}
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
    @com.google.gson.annotations.SerializedName *;
}
-keep class com.example.data.remote.** { *; }
-keep class com.example.model.** { *; }

# Network and reflection safety
-keepattributes Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn kotlin.Metadata
