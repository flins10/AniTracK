import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

sourceSets {
    main {
        resources.srcDirs("src/main/resources", "../app/src/main/res")
    }
}

tasks.withType<Copy>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register("syncWebAssets") {
    doLast {
        project.copy {
            from("../app/src/main/res/drawable") {
                include("*.png")
            }
            into("../public/assets")
        }
        project.copy {
            from("../app/src/main/res/drawable") {
                include("*.png")
            }
            into("../assets")
        }
        project.copy {
            from("../app/src/main/res/drawable") {
                include("*.png")
            }
            into("src/main/resources/drawable")
        }
        project.copy {
            from("../public") {
                include("index.html", "app.js", "styles.css", "sw.js", "manifest.json", "downloads-config.json", "assets-bundle.js")
            }
            into("..")
        }
        project.copy {
            from("../public/assets/aistudio") {
                include("117029-removebg-preview_a7bd2144.png", "s_2a7b087c.mp3", "s1_9f22134b.mp3", "s2_1eb199af.mp3", "s3_186666cd.mp3", "s4_f2c388e7.mp3", "s5_208121d9.mp3", "s6_5675dac2.mp3", "s7_9691e270.mp3", "s8_653b32e5.mp3", "s9_19fa5bf7.mp3")
            }
            into("../assets/aistudio")
        }
        project.copy {
            from("../public/assets/aistudio") {
                include("117029-removebg-preview_a7bd2144.png", "s_2a7b087c.mp3", "s1_9f22134b.mp3", "s2_1eb199af.mp3", "s3_186666cd.mp3", "s4_f2c388e7.mp3", "s5_208121d9.mp3", "s6_5675dac2.mp3", "s7_9691e270.mp3", "s8_653b32e5.mp3", "s9_19fa5bf7.mp3")
            }
            into("src/main/resources/sandrone")
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("javazoom:jlayer:1.0.1")
}

compose.desktop {
    application {
        mainClass = "com.example.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Exe,
                TargetFormat.Dmg,
                TargetFormat.Msi
            )
            packageName = "AnimeUniverse"
            packageVersion = "2.1.0"
            description = "Anime Universe — Track anime. Share taste."
            vendor = "AI Studio"

            windows {
                dirChooser = true
                shortcut = true
                iconFile.set(project.file("src/main/resources/icons/app_icon.ico"))
            }
            macOS {
                bundleID = "com.aistudio.animeuniverse"
                dockName = "Anime Universe"
                iconFile.set(project.file("src/main/resources/icons/app_icon.icns"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/app_icon.png"))
            }
        }
    }
}
