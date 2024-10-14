plugins {
    kotlin("jvm") version "2.0.20"
}


group = "io.wongaz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1") // Or your preferred serialization format
    implementation("com.charleskorn.kaml:kaml:0.53.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}