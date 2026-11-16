plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.anitrack.knyw"
    minSdk = 26
    targetSdk = 36
    versionCode = (project.findProperty("versionCode")?.toString()?.toIntOrNull()) ?: 3
    versionName = (project.findProperty("versionName")?.toString()) ?: "2.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = true
    includeInBundle = true
  }
}

abstract class GenerateObfuscatedVaultTask : DefaultTask() {
  @get:org.gradle.api.tasks.Optional
  @get:org.gradle.api.tasks.InputFile
  abstract val envFile: RegularFileProperty

  @get:org.gradle.api.tasks.Input
  abstract val envSupabaseUrl: Property<String>

  @get:org.gradle.api.tasks.Input
  abstract val envSupabaseKey: Property<String>

  @get:org.gradle.api.tasks.OutputDirectory
  abstract val outputDir: DirectoryProperty

  @org.gradle.api.tasks.TaskAction
  fun generate() {
    val envMap = mutableMapOf<String, String>()
    val fileRef = envFile.orNull?.asFile
    if (fileRef != null && fileRef.exists()) {
      fileRef.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
          val eq = trimmed.indexOf('=')
          if (eq > 0) {
            val k = trimmed.substring(0, eq).trim()
            val v = trimmed.substring(eq + 1).trim().removeSurrounding("\"").removeSurrounding("'")
            envMap[k] = v
          }
        }
      }
    }
    fun resolveSecret(mapVal: String?, propVal: String, placeholderMarker: String): String {
      val candidate = mapVal?.takeIf { it.isNotBlank() }
        ?: propVal.takeIf { it.isNotBlank() }
        ?: ""
      return if (candidate.contains(placeholderMarker, ignoreCase = true)) "" else candidate.trim()
    }

    val rawUrl = resolveSecret(envMap["SUPABASE_URL"], envSupabaseUrl.get(), "your-project.supabase.co").trimEnd('/')
    val rawKey = resolveSecret(envMap["SUPABASE_ANON_KEY"], envSupabaseKey.get(), "your-supabase")

    fun encodeShards(input: String, seedByte: Int): Triple<List<Int>, List<Int>, List<Int>> {
      val bytes = input.toByteArray(Charsets.UTF_8)
      val s0 = mutableListOf<Int>()
      val s1 = mutableListOf<Int>()
      val s2 = mutableListOf<Int>()
      for (i in bytes.indices) {
        val salt = ((i * 73 + seedByte * 31 + 19) xor 0x5A) and 0xFF
        val masked = (bytes[i].toInt() and 0xFF) xor salt
        when (i % 3) {
          0 -> s0.add(masked)
          1 -> s1.add(masked)
          else -> s2.add(masked)
        }
      }
      return Triple(s0, s1, s2)
    }

    val defaultU0 = listOf(142, 189, 7, 111, 21, 58, 232, 139, 161, 70, 101, 29, 63, 238)
    val defaultU1 = listOf(43, 201, 206, 190, 74, 112, 5, 42, 241, 204, 185, 85, 61)
    val defaultU2 = listOf(96, 73, 56, 236, 149, 160, 65, 121, 4, 40, 231, 158, 171)
    val defaultULen = 40

    val defaultK0 = listOf(33, 201, 136, 171, 66, 89, 93, 40, 211, 176, 163, 127, 113, 73, 97, 192)
    val defaultK1 = listOf(105, 3, 52, 217, 130, 142, 98, 95, 28, 103, 247, 138, 144, 105, 66)
    val defaultK2 = listOf(159, 77, 121, 19, 3, 255, 136, 136, 105, 67, 65, 20, 182, 173, 170)
    val defaultKLen = 46

    val (u0, u1, u2) = if (rawUrl.isNotBlank()) encodeShards(rawUrl, 0x37) else Triple(defaultU0, defaultU1, defaultU2)
    val uLen = if (rawUrl.isNotBlank()) rawUrl.toByteArray(Charsets.UTF_8).size else defaultULen

    val (k0, k1, k2) = if (rawKey.isNotBlank()) encodeShards(rawKey, 0x6B) else Triple(defaultK0, defaultK1, defaultK2)
    val kLen = if (rawKey.isNotBlank()) rawKey.toByteArray(Charsets.UTF_8).size else defaultKLen

    val outFile = outputDir.file("com/example/config/VaultMaterial.kt").get().asFile
    outFile.parentFile.mkdirs()
    outFile.writeText(
      """
      package com.example.config

      internal object VaultMaterial {
          private val u0 = intArrayOf(${u0.joinToString(", ")})
          private val u1 = intArrayOf(${u1.joinToString(", ")})
          private val u2 = intArrayOf(${u2.joinToString(", ")})
          private val k0 = intArrayOf(${k0.joinToString(", ")})
          private val k1 = intArrayOf(${k1.joinToString(", ")})
          private val k2 = intArrayOf(${k2.joinToString(", ")})

          fun decodeEndpoint(): String = reconstruct(u0, u1, u2, $uLen, 0x37)
          fun decodeCredential(): String = reconstruct(k0, k1, k2, $kLen, 0x6B)

          private fun reconstruct(s0: IntArray, s1: IntArray, s2: IntArray, len: Int, seedByte: Int): String {
              if (len <= 0) return ""
              val out = ByteArray(len)
              var i0 = 0
              var i1 = 0
              var i2 = 0
              for (i in 0 until len) {
                  val masked = when (i % 3) {
                      0 -> s0[i0++]
                      1 -> s1[i1++]
                      else -> s2[i2++]
                  }
                  val salt = ((i * 73 + seedByte * 31 + 19) xor 0x5A) and 0xFF
                  out[i] = (masked xor salt).toByte()
              }
              return String(out, Charsets.UTF_8)
          }
      }
      """.trimIndent() + "\n"
    )
  }
}

androidComponents {
  onVariants { variant ->
    val capName = variant.name.replaceFirstChar { it.uppercase() }
    val vaultTask = tasks.register<GenerateObfuscatedVaultTask>("generate${capName}ObfuscatedVault") {
      val rootEnv = rootProject.file(".env")
      if (rootEnv.exists()) {
        envFile.set(rootEnv)
      }
      envSupabaseUrl.set(System.getenv("SUPABASE_URL") ?: "")
      envSupabaseKey.set(System.getenv("SUPABASE_ANON_KEY") ?: "")
    }
    variant.sources.kotlin?.addGeneratedSourceDirectory(
      vaultTask,
      GenerateObfuscatedVaultTask::outputDir
    )
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("SUPABASE_URL")
  ignoreList.add("SUPABASE_ANON_KEY")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.datastore.preferences)
  // Removed Media3 streaming dependencies (streaming feature completely removed)
  // implementation(libs.androidx.media3.exoplayer)
  // implementation(libs.androidx.media3.exoplayer.hls)
  // implementation(libs.androidx.media3.exoplayer.dash)
  // implementation(libs.androidx.media3.ui)
  // implementation(libs.androidx.media3.common)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.gson)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  // implementation(libs.tukaani.xz)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
