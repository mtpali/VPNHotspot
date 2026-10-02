import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.wire)
}

abstract class GenerateGitJavaTask : DefaultTask() {
    @get:Input
    abstract val includeStatus: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    abstract val repositoryDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val repositoryDir = repositoryDir.get().asFile
        val gitSha = try {
            ProcessBuilder("git", "rev-parse", "HEAD").directory(repositoryDir).redirectErrorStream(true).start().run {
                inputStream.bufferedReader().readText().trimEnd().takeIf { waitFor() == 0 }
            }
        } catch (_: Exception) {
            null
        }
        val gitStatus = if (includeStatus.get()) try {
            ProcessBuilder("git", "status", "--porcelain=v1").directory(repositoryDir).redirectErrorStream(true)
                .start().run { inputStream.bufferedReader().readText().trimEnd().takeIf { waitFor() == 0 } }
        } catch (_: Exception) {
            null
        } else null
        outputDir.file("be/mygod/vpnhotspot/BuildGit.java").get().asFile.apply {
            parentFile.mkdirs()
            writeText("""
                package be.mygod.vpnhotspot;
                public final class BuildGit {
                    public static final String VALUE = ${JsonOutput.toJson(
                if (gitSha.isNullOrEmpty()) "" else if (gitStatus.isNullOrEmpty()) gitSha else "$gitSha\n$gitStatus")};
                    private BuildGit() {}
                }
            """.trimIndent() + "\n")
        }
    }
}

abstract class BuildDaemonNativeLibsTask : DefaultTask() {
    @get:Internal
    abstract val sourceDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val cargoSources: FileTree
        get() = project.fileTree(sourceDir).matching { exclude("target/**") }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val protoDir: DirectoryProperty

    @get:Input
    abstract val cargoProfile: Property<String>

    @get:Input
    abstract val androidPlatform: Property<Int>

    @get:Input
    abstract val targetAbis: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    abstract val targetDir: DirectoryProperty

    @TaskAction
    fun build() {
        val cargoDir = sourceDir.get().asFile
        val targetDir = targetDir.get().asFile
        outputDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }
        val profile = cargoProfile.get()
        val rustFlags = listOf(
            "--remap-path-prefix=${System.getProperty("user.home").trimEnd('/')}/=",
            "--remap-path-prefix=${cargoDir.absolutePath}=.",
            "--remap-path-prefix=${cargoDir.absolutePath}/=",
        ).joinToString("\u001F")
        val targets = mapOf(
            "arm64-v8a" to "aarch64-linux-android",
            "armeabi-v7a" to "armv7-linux-androideabi",
            "x86_64" to "x86_64-linux-android",
        )
        for (abi in targetAbis.get()) {
            val target = targets.getValue(abi)
            val command = mutableListOf("cargo", "ndk", "--target", abi, "--platform", androidPlatform.get().toString(),
                "build", "--locked", "--bin", "vpnhotspotd").apply {
                if (profile == "release") add("--release")
            }
            val process = ProcessBuilder(command).directory(cargoDir).redirectErrorStream(true).apply {
                environment().run {
                    this["CARGO_BUILD_TARGET_DIR"] = targetDir.absolutePath
                    this["CARGO_ENCODED_RUSTFLAGS"] = rustFlags
                }
            }.start()
            val output = process.inputStream.bufferedReader().readText()
            check(process.waitFor() == 0) {
                "cargo build failed for $target\n$output"
            }
            val binary = targetDir.resolve("$target/$profile/vpnhotspotd")
            check(binary.isFile) { "Missing daemon binary: ${binary.absolutePath}" }
            outputDir.file("$abi/libvpnhotspotd.so").get().asFile.apply {
                parentFile.mkdirs()
                binary.copyTo(this, overwrite = true)
            }
        }
    }
}

val javaVersion = 11
val requestedAbis = providers.gradleProperty("targetAbis").orElse("armeabi-v7a,arm64-v8a")
    .map { it.split(',').also { abis ->
        require(abis.isNotEmpty() && abis.all { abi -> abi in listOf("armeabi-v7a", "arm64-v8a", "x86_64") }) {
            "targetAbis must contain supported Android ABIs"
        }
    } }
android {
    namespace = "be.mygod.vpnhotspot"

    compileOptions {
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
    }
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig {
        applicationId = "be.mygod.vpnhotspot"
        minSdk = 28
        targetSdk = 37
        versionCode = 2015
        versionName = "3.0.8-oled.3"
        resourceConfigurations += "en"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    splits {
        abi {
            isEnable = true
            reset()
            include(*requestedAbis.get().toTypedArray())
            isUniversalApk = false
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            // BuildGit already records the commit; AGP's VCS tag task fails when Git packs branch refs.
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging.resources.excludes += listOf(
        "**/*.kotlin_*",
        "META-INF/versions/**",
    )
    // API 28's linker cannot run executables directly from an APK ZIP entry.
    packaging.jniLibs.useLegacyPackaging = true
    lint.warning += "FullBackupContent"
    lint.warning += "UnsafeOptInUsageError"
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
}
val hiddenApiStubAnnotations = configurations.create("hiddenApiStubAnnotations")
val compileHiddenApiStubs = tasks.register<JavaCompile>("compileHiddenApiStubs") {
    source("src/hiddenApiStubs/java")
    classpath = files(androidComponents.sdkComponents.bootClasspath) + hiddenApiStubAnnotations
    destinationDirectory.set(layout.buildDirectory.dir("intermediates/hiddenApiStubs/classes"))
    sourceCompatibility = javaVersion.toString()
    targetCompatibility = javaVersion.toString()
    options.release.set(javaVersion)
}
val hiddenApiStubsClasses = files(compileHiddenApiStubs.flatMap { it.destinationDirectory })
    .builtBy(compileHiddenApiStubs)
val hiddenApiStubsJar = tasks.register<Jar>("hiddenApiStubsJar") {
    from(hiddenApiStubsClasses)
    archiveFileName.set("hidden-api-stubs.jar")
}
wire {
    kotlin {
        enumMode = "sealed_class"
        rpcRole = "none"
    }
}
androidComponents.onVariants { variant ->
    val variantTitle = variant.name.replaceFirstChar(Char::titlecase)
    val task = tasks.register<GenerateGitJavaTask>("generate${variantTitle}GitJava") {
        includeStatus.set(variant.buildType == "debug")
        outputDir.set(layout.buildDirectory.dir("generated/source/git/${variant.name}"))
        repositoryDir.set(rootProject.layout.projectDirectory)
        outputs.upToDateWhen { false }
    }
    variant.sources.java?.addGeneratedSourceDirectory(task, GenerateGitJavaTask::outputDir)
    val daemonTask = tasks.register<BuildDaemonNativeLibsTask>(
        "build${variantTitle}DaemonNativeLibs") {
        sourceDir.set(layout.projectDirectory.dir("src/main/rust/vpnhotspotd"))
        protoDir.set(layout.projectDirectory.dir("src/main/proto"))
        cargoProfile.set(if (variant.buildType == "release") "release" else "debug")
        androidPlatform.set(android.defaultConfig.minSdk!!)
        targetAbis.set(requestedAbis)
        outputDir.set(layout.buildDirectory.dir("generated/nativeLibs/daemon/${variant.name}"))
        targetDir.set(layout.buildDirectory.dir("rust/vpnhotspotd"))
    }
    variant.sources.jniLibs?.addGeneratedSourceDirectory(daemonTask, BuildDaemonNativeLibsTask::outputDir)
}
ksp {
    arg("room.expandProjection", "true")
    arg("room.incremental", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    compileOnly(files(hiddenApiStubsJar))
    ksp(libs.room.compiler)
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3.adaptive:adaptive")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.activity.compose)
    implementation(libs.core)
    implementation(libs.hiddenapibypass)
    implementation(libs.ktor.io.jvm)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.librootkotlinx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.ktx)
    implementation(libs.timber)
    implementation(libs.wire.runtime)
    implementation(libs.zxing.core)
    debugImplementation("androidx.compose.ui:ui-tooling")
    hiddenApiStubAnnotations(libs.annotation.jvm)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.junit.ktx)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation("androidx.test:core:1.7.0")
}
