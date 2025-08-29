plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    version = "22.0.2" // or your installed JavaFX version
    modules = listOf("javafx.controls", "javafx.graphics")
}

application {
    mainClass.set("ParticleSim3DAppKt")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(16)
}