import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "com.aistudio.anitrack"
version = "2.1.0"

abstract class GenerateDesktopVaultTask : DefaultTask() {
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

        val outFile = outputDir.file("com/example/desktop/VaultMaterial.kt").get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(
            """
            package com.example.desktop

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

val generateVaultTask = tasks.register<GenerateDesktopVaultTask>("generateDesktopObfuscatedVault") {
    val parentEnv = project.file("../.env")
    val localEnv = project.file(".env")
    if (parentEnv.exists()) {
        envFile.set(parentEnv)
    } else if (localEnv.exists()) {
        envFile.set(localEnv)
    }
    envSupabaseUrl.set(System.getenv("SUPABASE_URL") ?: "")
    envSupabaseKey.set(System.getenv("SUPABASE_ANON_KEY") ?: "")
    outputDir.set(layout.buildDirectory.dir("generated/source/vault/main"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    sourceSets["main"].kotlin.srcDir(generateVaultTask.map { it.outputDir })
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
}

compose.desktop {
    application {
        mainClass = "com.example.desktop.MainDesktopKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Exe,
                TargetFormat.Msi,
                TargetFormat.Dmg
            )
            packageName = "AnimeUniverse"
            packageVersion = "2.1.0"
            description = "Anime Universe — Track Anime & Share Taste"
            vendor = "Anime Universe"
            includeAllModules = true

            windows {
                menuGroup = "Anime Universe"
                shortcut = true
                dirChooser = true
                upgradeUuid = "2f12567d-9632-4773-a78c-6168c8cde47c"
            }

            macOS {
                bundleID = "com.aistudio.anitrack.knyw.desktop"
                dockName = "Anime Universe"
            }
        }
    }
}
