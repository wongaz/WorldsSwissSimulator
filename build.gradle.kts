plugins {
    kotlin("jvm") version "2.0.20"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}


group = "io.wongaz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1") // Or your preferred serialization format

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")


    implementation("org.jgrapht:jgrapht-core:1.5.2")
    implementation("org.jgrapht:jgrapht-io:1.5.2")

    ksp("me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.2")
    implementation("me.tatarka.inject:kotlin-inject-runtime:0.7.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}