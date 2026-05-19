plugins {
    kotlin("jvm") version "2.3.0"
    id("com.google.devtools.ksp") version "2.3.7"
    application
}


group = "io.wongaz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1") // Or your preferred serialization format

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("org.jgrapht:jgrapht-core:1.5.2")
    implementation("org.jgrapht:jgrapht-io:1.5.2")

    ksp("me.tatarka.inject:kotlin-inject-compiler-ksp:0.9.0")
    implementation("me.tatarka.inject:kotlin-inject-runtime:0.9.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("io.wongaz.MainKt")
}