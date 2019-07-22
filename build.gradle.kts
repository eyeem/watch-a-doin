import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.11"
    maven
}

group = "com.eyeem"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    testCompile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    testCompile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.1")

    testCompile("junit", "junit", "4.12")
    testCompile("org.amshove.kluent:kluent:1.34")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}