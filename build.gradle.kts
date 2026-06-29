
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    id("org.graalvm.buildtools.native") version "0.11.2"
}

group = "example.com"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("dev.inmo:tgbotapi:30.0.1")
    implementation("dev.inmo:tgbotapi.behaviour_builder:30.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")

    implementation("org.ktorm:ktorm-core:3.6.0")
    implementation("org.ktorm:ktorm-support-postgresql:3.6.0")
    
    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.2")
    
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

kotlin {
    jvmToolchain(17)
}

tasks.register<JavaExec>("dbInspect") {
    group = "application"
    description = "Inspect PostgreSQL schema and data"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("kirillale.lakinais.tools.DbInspectKt")
}

tasks.register<JavaExec>("dbResetBookingData") {
    group = "application"
    description = "Delete all bookings and schedules (dry-run; add -Pexecute to delete)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("kirillale.lakinais.tools.DbResetBookingDataKt")
    if (project.hasProperty("execute")) {
        systemProperty("execute", "true")
    }
}

tasks.register<JavaExec>("cleanTestData") {
    group = "application"
    description = "Remove test accounts/bookings (dry-run; add -Pexecute to delete)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("kirillale.lakinais.tools.CleanTestDataKt")
    if (project.hasProperty("execute")) {
        systemProperty("execute", "true")
    }
}

tasks.test {
    filter {
        excludeTestsMatching("kirillale.lakinais.db.BookingOverlapDbTest")
    }
}

tasks.register<Test>("dbTest") {
    group = "verification"
    description = "PostgreSQL integration tests (./gradlew dbTest -PrunDbTests)"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("kirillale.lakinais.db.BookingOverlapDbTest")
    }
    onlyIf { project.hasProperty("runDbTests") }
}