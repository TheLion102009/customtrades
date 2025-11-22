plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "de.customtrades"
version = "1.1.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.rosewooddev.io/repository/public/") // PlayerPoints
    maven("https://repo.nexomc.com/releases/") // Nexo
    maven("https://jitpack.io")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // External APIs
    compileOnly("org.black_ixx:playerpoints:3.2.7")
    compileOnly("com.nexomc:nexo:0.1.0")

    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("CustomTrades-${project.version}.jar")

        relocate("kotlin", "de.customtrades.libs.kotlin")
        relocate("kotlinx", "de.customtrades.libs.kotlinx")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

kotlin {
    jvmToolchain(21)
}