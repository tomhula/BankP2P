plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

group = "cz.tomashula"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.datetime)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)
}

application {
    mainClass = "cz.tomashula.bankp2p.MainKt"
}

tasks.shadowJar {
    archiveFileName = "${project.name}-${project.version}-executable.jar"
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
