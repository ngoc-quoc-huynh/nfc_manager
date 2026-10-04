import org.gradle.process.CommandLineArgumentProvider

group = "dev.huynh.nfc_manager_android"
version = "1.0"

buildscript {
    val kotlinVersion = "2.4.20"
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.4.1")
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
    id("org.jetbrains.kotlin.android")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
}

tasks.withType<Test>().configureEach {
    val mockitoAgent = objects.newInstance<MockitoAgentArgumentProvider>()
    mockitoAgent.agentJar.from(providers.provider {
        classpath.filter { it.name.startsWith("mockito-core-") && it.extension == "jar" }
    })
    jvmArgumentProviders.add(mockitoAgent)
}
