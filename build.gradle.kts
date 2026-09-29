plugins {
    application
    kotlin("jvm") version "2.3.21"
    kotlin("multiplatform") version "2.3.21" apply false
    kotlin("plugin.serialization") version "2.3.21" apply false
    kotlin("plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.12"
}


group = "io.wongaz"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("io.wongaz.ServerMainKt")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":shared"))
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
    implementation("org.postgresql:postgresql:42.7.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("io.ktor:ktor-server-netty:3.3.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-server-status-pages:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("org.jgrapht:jgrapht-core:1.5.2")
    implementation("org.jgrapht:jgrapht-io:1.5.2")

    ksp("me.tatarka.inject:kotlin-inject-compiler-ksp:0.9.0")
    implementation("me.tatarka.inject:kotlin-inject-runtime:0.9.0")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.ktor:ktor-server-test-host:3.3.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val bundleWeb by tasks.registering(Sync::class) {
    dependsOn(":web:jsBrowserDistribution")
    from(project(":web").layout.buildDirectory.dir("dist/js/productionExecutable"))
    into(layout.buildDirectory.dir("generated/webResources/web"))
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/webResources"))
}

tasks.processResources {
    dependsOn(bundleWeb)
}

tasks.check {
    dependsOn(":web:jsNodeTest", ":shared:allTests")
}

val integrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[integrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    add(integrationTest.implementationConfigurationName, "org.testcontainers:postgresql:1.21.4")
    add(integrationTest.implementationConfigurationName, "org.testcontainers:junit-jupiter:1.21.4")
}

tasks.register<Test>("integrationTest") {
    description = "Runs PostgreSQL integration tests against a disposable Docker container."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

tasks.register<JavaExec>("runCli") {
    description = "Runs the original console simulator without a database."
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.wongaz.MainKt")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}