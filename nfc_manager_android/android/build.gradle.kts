import org.gradle.process.CommandLineArgumentProvider

group = "dev.huynh.nfc_manager_android"
version = "1.0"

buildscript {
    // Fallbacks for hosts that do not provide AGP or KGP; host versions take precedence.
    // They match flutter/packages at the Flutter 3.44 floor. Update only when the floor
    // rises or a tooling fix requires it.
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

// Kotlin comes from AGP 9 built-in Kotlin, or from Flutter tooling applying KGP when it is off.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

abstract class MockitoAgentArgumentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val agentJar: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> = listOf("-javaagent:${agentJar.singleFile.absolutePath}")
}

android {
    namespace = "dev.huynh.nfc_manager_android"

    compileSdk = flutter.compileSdkVersion

    compileOptions {
        // Target Java 17 so consuming apps do not need JDK 21.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 24
    }

    lint {
        warningsAsErrors = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion")
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()

            it.testLogging {
                events("skipped", "failed")
            }
        }
    }
}

// Keep in sync with the mockito-core version resolved through mockito-kotlin.
val mockitoVersion = "5.23.0"

// Resolves only the mockito-core jar, which is loaded as a Java agent for unit tests.
val mockitoAgentConfiguration: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    mockitoAgentConfiguration("org.mockito:mockito-core:$mockitoVersion")
}

tasks.withType<Test>().configureEach {
    val mockitoAgent = objects.newInstance<MockitoAgentArgumentProvider>()
    mockitoAgent.agentJar.from(mockitoAgentConfiguration)
    jvmArgumentProviders.add(mockitoAgent)
}
