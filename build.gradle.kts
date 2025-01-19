import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.5"
    kotlin("jvm")
}

group = "com.kamikazejam"
version = "1.0.0.beta.9-SNAPSHOT"
description = "Simple Data Storage Solution using MongoDB"


repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.luxiouslabs.net/repository/maven-public/")
}

dependencies {
    // Spigot
    compileOnly("net.techcable.tacospigot:server:1.8.8-R0.2-REDUCED")

    // Dependencies
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("de.undercouch:bson4jackson:2.15.1")
    implementation("org.mongodb:mongodb-driver-sync:5.3.0")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // Testing Dependencies
    testImplementation("net.techcable.tacospigot:server:1.8.8-R0.2-REDUCED")

    // Jetbrains
    compileOnly("org.jetbrains:annotations:26.0.1")
    testCompileOnly("org.jetbrains:annotations:26.0.1")
    implementation(kotlin("stdlib-jdk8"))

    // Kotlin Libraries
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}

// Register a task to delete the jars in the libs folder
tasks.register<Delete>("cleanLibs") {
    delete("build/libs")
}

tasks {
    publish.get().dependsOn(build)
    build.get().dependsOn(shadowJar)
    shadowJar.get().dependsOn("cleanLibs")

    shadowJar {
        archiveClassifier.set("")
        relocate("ch.qos.logback", "com.kamikazejam.datastore.internal.logback")
        relocate("com.fasterxml.jackson", "com.kamikazejam.datastore.internal.jackson")
        relocate("com.mongodb", "com.kamikazejam.datastore.internal.mongodb")
        relocate("org.bson", "com.kamikazejam.datastore.internal.bson")
        relocate("org.slf4j", "com.kamikazejam.datastore.internal.slf4j")
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to rootProject.name,
            "version" to project.version,
            "description" to project.description,
            "date" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

// We want UTF-8 for everything
tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
}
tasks.withType<Javadoc> {
    options.encoding = Charsets.UTF_8.name()
    charset("UTF-8")
}
// Java 21
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = rootProject.group.toString()
            artifactId = project.name
            version = rootProject.version.toString()
            from(components["java"])
        }
    }

    repositories {
        maven {
            credentials {
                username = System.getenv("LUXIOUS_NEXUS_USER")
                password = System.getenv("LUXIOUS_NEXUS_PASS")
            }
            // Select URL based on version (if it's a snapshot or not)
            url = if (!project.version.toString().endsWith("-SNAPSHOT")) {
                uri("https://repo.luxiouslabs.net/repository/maven-releases/")
            } else {
                uri("https://repo.luxiouslabs.net/repository/maven-snapshots/")
            }
        }
    }
}