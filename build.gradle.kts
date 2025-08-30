plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"

}

val lwjglVersion = "3.3.4"

// Pick the right natives automatically (Win/Linux/macOS x64/ARM64)
val os = org.gradle.internal.os.OperatingSystem.current()
val arch = System.getProperty("os.arch").lowercase()
val lwjglNatives = when {
    os.isWindows -> "natives-windows"
    os.isLinux && ("aarch64" in arch || "arm64" in arch) -> "natives-linux-arm64"
    os.isLinux -> "natives-linux"
    os.isMacOsX && ("aarch64" in arch || "arm64" in arch) -> "natives-macos-arm64"
    os.isMacOsX -> "natives-macos"
    else -> error("Unsupported OS/arch: ${os.name} $arch")
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

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-assimp")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-assimp::$lwjglNatives")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}