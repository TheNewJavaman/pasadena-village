import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.7.20"
}

group = "net.javaman"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.microutils", "kotlin-logging-jvm", "3.0.2")
    implementation("ch.qos.logback", "logback-classic", "1.4.4")
    implementation("org.seleniumhq.selenium", "selenium-chrome-driver", "4.5.2")
    implementation("io.ktor", "ktor-client-cio", "2.1.2")
    implementation("io.github.resilience4j", "resilience4j-ratelimiter", "1.7.1")
    implementation("io.github.resilience4j", "resilience4j-kotlin", "1.7.1")
    implementation("org.jsoup", "jsoup", "1.15.3")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.13.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "18"
}

application {
    mainClass.set("net.javaman.pasadenavillage.MainKt")
}
