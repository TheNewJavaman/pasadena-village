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
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.4")
    implementation("io.github.microutils", "kotlin-logging-jvm", "3.0.2")
    implementation("ch.qos.logback", "logback-classic", "1.4.4")
    implementation("org.seleniumhq.selenium", "selenium-java", "4.5.3")
    implementation("io.github.resilience4j", "resilience4j-ratelimiter", "1.7.1")
    implementation("io.github.resilience4j", "resilience4j-kotlin", "1.7.1")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.13.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "18"
}

application {
    mainClass.set("net.javaman.pasadenavillage.MainKt")
}
