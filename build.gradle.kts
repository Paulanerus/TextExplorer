import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "dev.paulee"
version = rootProject.extra["app.version"] as String

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":ui"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "dev.paulee.MainKt"

        buildTypes.release.proguard {
            isEnabled = false
        }

        jvmArgs += listOf(
            "-Dapi.version=${property("api.version")}",
            "-Dcore.version=${property("core.version")}",
            "-Dui.version=${property("ui.version")}",
            "-Dapp.version=${property("app.version")}"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TextExplorer"

        }
    }
}