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
            }
            macOS {
                bundleID = "com.aistudio.animeuniverse"
                dockName = "Anime Universe"
            }
        }
    }
}
