import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    groovy
}

group = "uk.co.kenfos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.apache.groovy:groovy:4.0.6")
    testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<GroovyCompile>("compileTestGroovy") {
    val compileKotlin = tasks.named<KotlinCompile>("compileTestKotlin")
    dependsOn(compileKotlin)
    classpath += files(compileKotlin.get().destinationDirectory)
}
