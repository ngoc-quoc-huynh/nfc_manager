import org.gradle.process.CommandLineArgumentProvider

group = "dev.huynh.nfc_manager_android"
version = "1.0"

buildscript {
    // These are upstream flutter/packages fallback pins, not Flutter tooling minimum versions.
    // Use host-provided AGP or KGP when present. Keep pins frozen with the matching Renovate rule.
    val kotlinVersion = "2.3.0"
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.13.1")
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

// On AGP 8, Flutter tooling applies KGP. With Flutter 3.44, set android.builtInKotlin=false
// when using AGP 9. AGP 9 built-in Kotlin requires Flutter 3.47 or later.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
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
