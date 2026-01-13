import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    kotlin("plugin.serialization") version "2.2.21"
}

val testImagesDir: String = project.file("src/commonTest/resources/test-images").absolutePath

kotlin {
    // SEB:TODO enable this when one of us has time to fix all the API visibility issues
    // explicitApi()

    compilerOptions {
         allWarningsAsErrors.set(true)
    }

    // Apply Native-specific opt-ins
    targets.withType<KotlinNativeTarget>().configureEach {
        compilerOptions {
            optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    // Global opt-ins
    sourceSets.all {
        languageSettings.apply {
            optIn("kotlin.uuid.ExperimentalUuidApi")
            optIn("kotlin.io.encoding.ExperimentalEncodingApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
            optIn("kotlin.time.ExperimentalTime")
            optIn("dev.whyoleg.cryptography.DelicateCryptographyApi")
        }
    }
    // Suppress expect/actual classes Beta warning
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.getByName("main") {
            val ffmpegKitCinterop by cinterops.creating {
                defFile("src/nativeInterop/cinterop/ffmpegkit.def")
                packageName("id.homebase.homebasekmppoc.media.ffmpegkit")
                
                // Determine which architecture content to use for headers (headers are usually same)
                val frameworkArch = if (iosTarget.konanTarget.name.contains("simulator")) {
                    "ios-arm64_x86_64-simulator"
                } else {
                    "ios-arm64_arm64e"
                }
                
                val libsDir = project.file("libs/ffmpegkit-bundled.xcframework").absolutePath
                val includeDirs = listOf(
                    "ffmpegkit.xcframework", "libavcodec.xcframework", "libavdevice.xcframework",
                    "libavfilter.xcframework", "libavformat.xcframework", "libavutil.xcframework",
                    "libswresample.xcframework", "libswscale.xcframework"
                ).map { framework ->
                    "-F$libsDir/$framework/$frameworkArch"
                }
                
                compilerOpts(includeDirs)
            }
        }

        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts("-lsqlite3")
            linkerOpts("-lz", "-lbz2", "-liconv") // FFmpeg dependencies
            
            // Link against the frameworks
            val libsDir = project.file("libs/ffmpegkit-bundled.xcframework").absolutePath
             val frameworkArch = if (iosTarget.konanTarget.name.contains("simulator")) {
                "ios-arm64_x86_64-simulator"
            } else {
                "ios-arm64_arm64e"
            }
            
            val frameworks = listOf(
                "ffmpegkit", "libavcodec", "libavdevice", "libavfilter", 
                "libavformat", "libavutil", "libswresample", "libswscale"
            )
            
            frameworks.forEach { fw ->
                linkerOpts("-F$libsDir/$fw.xcframework/$frameworkArch", "-framework", fw)
            }
            
            freeCompilerArgs += listOf("-Xbinary=bundleId=id.homebase.homebasekmppoc")
        }
        
        // Configure linker for test binaries to also find ffmpegkit frameworks
        iosTarget.binaries.all {
            if (this is org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable) {
                val libsDir = project.file("libs/ffmpegkit-bundled.xcframework").absolutePath
                val frameworkArch = if (iosTarget.konanTarget.name.contains("simulator")) {
                    "ios-arm64_x86_64-simulator"
                } else {
                    "ios-arm64_arm64e"
                }
                
                linkerOpts("-lsqlite3")
                linkerOpts("-lz", "-lbz2", "-liconv")
                
                val frameworks = listOf(
                    "ffmpegkit", "libavcodec", "libavdevice", "libavfilter", 
                    "libavformat", "libavutil", "libswresample", "libswscale"
                )
                
                frameworks.forEach { fw ->
                    linkerOpts("-F$libsDir/$fw.xcframework/$frameworkArch", "-framework", fw)
                }
                

                // Copy the frameworks to the output directory so dyld can find them
                // We resolve all paths outside doLast to avoid capturing 'project' or 'this' context which breaks Config Cache
                val binary = this
                val libsDirFile = project.file("libs/ffmpegkit-bundled.xcframework")
                
                linkTaskProvider.configure {
                    doLast {
                        val outputDir = binary.outputDirectory
                        val frameworksDir = File(outputDir, "Frameworks")
                        if (!frameworksDir.exists()) {
                            frameworksDir.mkdirs()
                        }
                        
                        frameworks.forEach { fw ->
                            val srcFramework = File(libsDirFile, "$fw.xcframework/$frameworkArch/$fw.framework")
                            val destFramework = File(frameworksDir, "$fw.framework")
                            
                            if (srcFramework.exists()) {
                                srcFramework.copyRecursively(destFramework, overwrite = true)
                            }
                        }
                    }
                }
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.ui)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(files("libs/ffmpeg-kit-lts-ndk-r27-16k.aar"))
            implementation(libs.smart.exception.java)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
             implementation(libs.androidx.lifecycle.runtimeCompose)
             implementation(libs.atomicfu)
             implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kermit)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.html.builder)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutinesSwing)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.mock)
        }
        androidUnitTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.robolectric)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.junit)
        }
        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

// Disable allWarningsAsErrors for metadata compilation tasks only
// This works around KLIB duplicate unique_name warnings (known KMP issue: KT-66568)
// https://youtrack.jetbrains.com/issue/KT-66568
// while keeping strict warnings for actual source compilation
tasks.matching { it.name.contains("KotlinMetadata") }.configureEach {
    if (this is org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>) {
        compilerOptions {
            allWarningsAsErrors.set(false)
        }
    }
}

// Inject TEST_IMAGES_DIR into all ios test tasks
tasks.withType<KotlinNativeTest>().configureEach {
    environment("TEST_IMAGES_DIR", testImagesDir)
    environment("SIMCTL_CHILD_TEST_IMAGES_DIR", testImagesDir)
}

tasks.named<Test>("desktopTest") {
    environment("TEST_IMAGES_DIR", testImagesDir)

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
    forkEvery = 1
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

compose.desktop {
    application {
        mainClass = "id.homebase.homebasekmppoc.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Odin KMP"
            packageVersion = "1.0.0"
        }
    }
}

android {
    namespace = "id.homebase.homebasekmppoc"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "id.homebase.homebasekmppoc"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                // 4. INJECT PATH INTO ANDROID (ROBOLECTRIC)
                it.environment("TEST_IMAGES_DIR", testImagesDir)

                it.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                    showStandardStreams = true
                    showExceptions = true
                    showCauses = true
                    showStackTraces = true
                }
                it.systemProperty("robolectric.enabledSdks", "33")
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                it.forkEvery = 1
                it.maxParallelForks = Runtime.getRuntime().availableProcessors()
            }
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

sqldelight {
    databases {
        create("OdinDatabase") {
            packageName.set("id.homebase.homebasekmppoc.lib.database")
            dialect(libs.sqldelight.sqlite338.dialect)
        }
    }
}