import org.gradle.process.CommandLineArgumentProvider

group = "dev.huynh.nfc_manager_android"
version = "1.0"

buildscript {
    // Flutter 3.35 fallbacks support older hosts; host versions take precedence.
    // Update only for a higher Flutter minimum or a required tooling fix.
    val kotlinVersion = "2.1.0"
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        // Keep AGP 8.9.1 for Flutter 3.35 compatibility; host apps can use newer AGP.
        classpath("com.android.tools.build:gradle:8.9.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.library")
}

// Use AGP 9+ built-in Kotlin unless disabled; otherwise apply the Kotlin Android plugin.
val agpMajor = com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.substringBefore('.').toInt()
val builtInKotlinEnabled = agpMajor >= 9 &&
    (providers.gradleProperty("android.builtInKotlin").orNull?.toBoolean() ?: true)

if (!builtInKotlinEnabled) {
    apply(plugin = "org.jetbrains.kotlin.android")
}

abstract class MockitoAgentArgumentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val agentJar: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> =
        listOf("-javaagent:${agentJar.singleFile.absolutePath}")
}

android {
    namespace = "dev.huynh.nfc_manager_android"

    compileSdk = 34

    compileOptions {
        // Target Java 17 so consuming apps do not need JDK 21.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 21
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            it.outputs.upToDateWhen { false }

            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
                showStandardStreams = true
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
}

tasks.withType<Test>().configureEach {
    val mockitoAgent = objects.newInstance<MockitoAgentArgumentProvider>()
    mockitoAgent.agentJar.from(providers.provider {
        classpath.filter { it.name.startsWith("mockito-core-") && it.extension == "jar" }
    })
    jvmArgumentProviders.add(mockitoAgent)
}
