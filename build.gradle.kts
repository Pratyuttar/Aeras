import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.compose") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.compose") version "1.9.3"
}

group = "io.github.pratyuttar"
version = "1.0.2"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material3:material3-window-size-class:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    // back handling (Escape key); only a runtime dependency of the UI module by default
    implementation("org.jetbrains.compose.ui:ui-backhandler:1.9.3")

    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.5")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.5")
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    implementation("io.insert-koin:koin-core:4.1.0")
    implementation("io.insert-koin:koin-compose:4.1.0")
    implementation("io.insert-koin:koin-compose-viewmodel:4.1.0")

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // one Windows call, to colour the native title bar like the app
    implementation("net.java.dev.jna:jna:5.17.0")
}

compose.desktop {
    application {
        mainClass = "io.github.pratyuttar.aeras.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Aeras"
            packageVersion = "1.0.2"
            description = "Video and audio downloader for Windows, built on yt-dlp"
            vendor = "Pratyuttar"
            licenseFile.set(project.file("LICENSE"))
            includeAllModules = true
            // Files placed in packaging/resources/windows (for example yt-dlp.exe and the FFmpeg
            // programs) are shipped inside the app. Without them the app fetches both on first start.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("packaging/resources"))

            windows {
                menuGroup = "Aeras"
                shortcut = true
                menu = true
                // Installed for all users, with administrator rights. A per-user install runs
                // without them, and Windows then refuses to remove or replace the files when the
                // app was put on a second drive ("Could not set file security ... Config.Msi").
                perUserInstall = false
                dirChooser = true
                upgradeUuid = "4513b33a-6b4e-4e57-bdc5-768bb7de7d6f"
                iconFile.set(project.file("packaging/icon.ico"))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Self-test: a short end-to-end check (test download, audio extraction, window picture).
// Everything it writes stays in .dev/, which is not part of the repository.
// ---------------------------------------------------------------------------------------------

val selfTestDir = layout.projectDirectory.dir(".dev/selftest")

tasks.register<JavaExec>("selfTest") {
    group = "verification"
    description = "Runs the app from the build tree in self-test mode"
    mainClass.set("io.github.pratyuttar.aeras.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    environment("AERAS_SELF_TEST", selfTestDir.asFile.absolutePath)
    environment("AERAS_DATA_DIR", selfTestDir.dir("data").asFile.absolutePath)
    environment("AERAS_HOME_DIR", selfTestDir.dir("home").asFile.absolutePath)
}

tasks.register<Exec>("selfTestDist") {
    group = "verification"
    description = "Runs the packaged application (the real .exe with its bundled Java runtime) in self-test mode"
    dependsOn("createDistributable")
    val out = layout.projectDirectory.dir(".dev/selftest-dist")
    workingDir = projectDir
    executable = layout.buildDirectory.file("compose/binaries/main/app/Aeras/Aeras.exe").get().asFile.absolutePath
    environment("AERAS_SELF_TEST", out.asFile.absolutePath)
    environment("AERAS_DATA_DIR", out.dir("data").asFile.absolutePath)
    environment("AERAS_HOME_DIR", out.dir("home").asFile.absolutePath)
}
