
plugins {
    kotlin("jvm") version "2.2.21"

}

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.inmo:tgbotapi:30.0.1")
//    implementation("dev.inmo:tgbotapi.longpolling:30.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
    implementation("org.ktorm:ktorm-core:3.6.0")
    implementation("org.ktorm:ktorm-support-postgresql:3.6.0")

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.2")
}
kotlin {
    jvmToolchain(17)
}