plugins {
    kotlin("jvm") version "1.7.21"
}

group = "uk.co.kenfos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.4")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}