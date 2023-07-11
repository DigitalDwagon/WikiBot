import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion.VERSION_17
import kotlin.text.Charsets.UTF_8


plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.freefair.lombok") version "8.0.1"
    kotlin("jvm") version "1.8.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(platform("com.google.cloud:libraries-bom:23.1.0"))
    implementation("com.google.cloud:google-cloud-storage:2.1.7")
    implementation("net.dv8tion:JDA:5.0.0-alpha.12")
    //compileOnly("org.projectlombok:lombok:1.18.26")
    //annotationProcessor("org.projectlombok:lombok:1.18.26")
}

group = "dev.digitaldragon"
version = "1.0-SNAPSHOT"
description = "DokuWikiDumperBot"
java.sourceCompatibility = JavaVersion.VERSION_17

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = UTF_8.name()

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    shadowJar {
        archiveFileName.set("ArchiveBot-shadow.jar")
        manifest.attributes["Main-Class"] = "dev.digitaldragon.ArchiveBot"
    }
    javadoc {
        options.encoding = UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = UTF_8.name() // We want UTF-8 for everything
    }
}