import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

group = "com.kamikazejam"
version = "1.0.0-SNAPSHOT"
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
    api("org.mongodb:mongodb-driver-sync:5.2.1")
    api("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    api("org.json:json:20241224")
    api("ch.qos.logback:logback-classic:1.5.16")

    // Testing Dependencies
    testImplementation("net.techcable.tacospigot:server:1.8.8-R0.2-REDUCED")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")

    // Jetbrains
    compileOnly("org.jetbrains:annotations:26.0.1")
    testCompileOnly("org.jetbrains:annotations:26.0.1")
}

tasks {
    publish.get().dependsOn(build)

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

// Register a task to delete the jars in the libs folder
tasks.register<Delete>("cleanLibs") {
    delete("build/libs")
}
tasks.build.get().dependsOn("cleanLibs")

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
            url = if (project.version.toString().endsWith("-SNAPSHOT")) {
                uri("https://repo.luxiouslabs.net/repository/maven-releases/")
            } else {
                uri("https://repo.luxiouslabs.net/repository/maven-snapshots/")
            }
        }
    }
}