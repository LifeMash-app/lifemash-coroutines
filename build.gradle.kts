import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    `java-library`
    `maven-publish`
}

group = "com.lifemash"
repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
    explicitApi()
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifactId = "lifemash-coroutines"
            pom {
                name.set("LifeMash Coroutines")
                description.set("LifeMash의 Kotlin Coroutines 및 Flow 공통 확장입니다.")
                url.set("https://github.com/LifeMash-hq/lifemash-coroutines")
                scm {
                    url.set("https://github.com/LifeMash-hq/lifemash-coroutines")
                    connection.set("scm:git:https://github.com/LifeMash-hq/lifemash-coroutines.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lifemash-hq/lifemash-coroutines")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
