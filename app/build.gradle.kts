import java.security.MessageDigest

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
      isMinifyEnabled = true
      isShrinkResources = true
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

    fun decodeLegacy(s0: List<Int>, s1: List<Int>, s2: List<Int>, len: Int, seedByte: Int): ByteArray {
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
      return out
    }

    val defaultU0 = listOf(142, 189, 7, 111, 21, 58, 232, 139, 161, 70, 101, 29, 63, 238)
    val defaultU1 = listOf(43, 201, 206, 190, 74, 112, 5, 42, 241, 204, 185, 85, 61)
    val defaultU2 = listOf(96, 73, 56, 236, 149, 160, 65, 121, 4, 40, 231, 158, 171)
    val defaultK0 = listOf(33, 201, 136, 171, 66, 89, 93, 40, 211, 176, 163, 127, 113, 73, 97, 192)
    val defaultK1 = listOf(105, 3, 52, 217, 130, 142, 98, 95, 28, 103, 247, 138, 144, 105, 66)
    val defaultK2 = listOf(159, 77, 121, 19, 3, 255, 136, 136, 105, 67, 65, 20, 182, 173, 170)

    val urlBytes = if (rawUrl.isNotBlank()) rawUrl.toByteArray(Charsets.UTF_8) else decodeLegacy(defaultU0, defaultU1, defaultU2, 40, 0x37)
    val keyBytes = if (rawKey.isNotBlank()) rawKey.toByteArray(Charsets.UTF_8) else decodeLegacy(defaultK0, defaultK1, defaultK2, 46, 0x6B)

    // Deterministic per-secret nonce derived via SHA-256 + multi-round bit-rotated 4-shard diffusion
    val md = MessageDigest.getInstance("SHA-256")
    val seedMaterial = md.digest(("au-vault-v2|" + urlBytes.size + "|" + keyBytes.size + "|com.aistudio.anitrack.knyw").toByteArray(Charsets.UTF_8))
    val nonceA = seedMaterial.copyOfRange(0, 16)
    val nonceB = seedMaterial.copyOfRange(16, 32)

    fun rotLeft8(v: Int, bits: Int): Int = ((v shl bits) or (v ushr (8 - bits))) and 0xFF

    fun sealShards(plain: ByteArray, nonce: ByteArray, domainTag: String): List<List<Int>> {
      val digest = MessageDigest.getInstance("SHA-256")
      val stream = ByteArray(plain.size)
      var produced = 0
      var blockIdx = 0
      while (produced < plain.size) {
        digest.reset()
        digest.update(nonce)
        digest.update(domainTag.toByteArray(Charsets.UTF_8))
        digest.update(byteArrayOf((blockIdx ushr 8).toByte(), (blockIdx and 0xFF).toByte()))
        val blk = digest.digest()
        val take = minOf(blk.size, plain.size - produced)
        System.arraycopy(blk, 0, stream, produced, take)
        produced += take
        blockIdx++
      }
      val shards = List(4) { mutableListOf<Int>() }
      for (i in plain.indices) {
        val p = plain[i].toInt() and 0xFF
        val k1 = stream[i].toInt() and 0xFF
        val k2 = nonce[i % nonce.size].toInt() and 0xFF
        val rot = (i % 5) + 1
        val mixed = rotLeft8(p xor k1, rot) xor ((k2 + i * 17) and 0xFF)
        shards[i % 4].add(mixed)
      }
      return shards
    }

    val uShards = sealShards(urlBytes, nonceA, "ep:v2")
    val kShards = sealShards(keyBytes, nonceB, "cr:v2")
    val nonceALiterals = nonceA.joinToString(", ") { b -> "(${b.toInt()}).toByte()" }
    val nonceBLiterals = nonceB.joinToString(", ") { b -> "(${b.toInt()}).toByte()" }

    val outFile = outputDir.file("com/example/config/VaultMaterial.kt").get().asFile
    outFile.parentFile.mkdirs()
    outFile.writeText(
      """
      package com.example.config

      import java.security.MessageDigest

      internal object VaultMaterial {
          private val nA = byteArrayOf($nonceALiterals)
          private val nB = byteArrayOf($nonceBLiterals)
          private val u0 = intArrayOf(${uShards[0].joinToString(", ")})
          private val u1 = intArrayOf(${uShards[1].joinToString(", ")})
          private val u2 = intArrayOf(${uShards[2].joinToString(", ")})
          private val u3 = intArrayOf(${uShards[3].joinToString(", ")})
          private val k0 = intArrayOf(${kShards[0].joinToString(", ")})
          private val k1 = intArrayOf(${kShards[1].joinToString(", ")})
          private val k2 = intArrayOf(${kShards[2].joinToString(", ")})
          private val k3 = intArrayOf(${kShards[3].joinToString(", ")})

          fun decodeEndpoint(): String = unseal(arrayOf(u0, u1, u2, u3), ${urlBytes.size}, nA, "ep:v2")
          fun decodeCredential(): String = unseal(arrayOf(k0, k1, k2, k3), ${keyBytes.size}, nB, "cr:v2")

          private fun rotRight8(v: Int, bits: Int): Int = ((v ushr bits) or (v shl (8 - bits))) and 0xFF

          private fun unseal(shards: Array<IntArray>, len: Int, nonce: ByteArray, tag: String): String {
              if (len <= 0) return ""
              val md = MessageDigest.getInstance("SHA-256")
              val stream = ByteArray(len)
              var produced = 0
              var blkIdx = 0
              while (produced < len) {
                  md.reset()
                  md.update(nonce)
                  md.update(tag.toByteArray(Charsets.UTF_8))
                  md.update(byteArrayOf((blkIdx ushr 8).toByte(), (blkIdx and 0xFF).toByte()))
                  val blk = md.digest()
                  val take = minOf(blk.size, len - produced)
                  System.arraycopy(blk, 0, stream, produced, take)
                  produced += take
                  blkIdx++
              }
              val out = ByteArray(len)
              val idx = IntArray(4)
              for (i in 0 until len) {
                  val lane = i % 4
                  val c = shards[lane][idx[lane]++]
                  val k2 = nonce[i % nonce.size].toInt() and 0xFF
                  val unmasked = c xor ((k2 + i * 17) and 0xFF)
                  val rot = (i % 5) + 1
                  val k1 = stream[i].toInt() and 0xFF
                  out[i] = (rotRight8(unmasked, rot) xor k1).toByte()
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
