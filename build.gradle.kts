import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.72"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.72"
    maven
}

group = "com.eyeem"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = "0.12.0")

    testApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    testApi("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.1")
    testApi("com.google.code.gson:gson:2.7")

    testApi("junit", "junit", "4.12")
    testApi("org.amshove.kluent:kluent:1.34")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs= listOf("-Xopt-in=kotlin.RequiresOptIn")
}
