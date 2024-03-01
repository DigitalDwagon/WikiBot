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
    // https://mvnrepository.com/artifact/org.kitteh.irc/client-lib
    implementation("org.kitteh.irc:client-lib:8.0.0")
    implementation("org.json:json:20230227")
    implementation("com.google.code.gson:gson:2.10.1") //yes there are two json libraries, one for receiving and one for sending. it's stupid but i'm lazy
    // https://mvnrepository.com/artifact/commons-io/commons-io
    //implementation("commons-io:commons-io:2.13.0")

    //used for XML parsing in link extractor.
    implementation("org.jsoup:jsoup:1.16.1")

    //used to decompress zstd streams
    // https://mvnrepository.com/artifact/com.github.luben/zstd-jni
    implementation("com.github.luben:zstd-jni:1.5.5-5")

    //web library for dashboard
    implementation("io.javalin:javalin:5.6.3")

    // https://mvnrepository.com/artifact/com.beust/jcommander
    implementation("com.beust:jcommander:1.82")

    //https://github.com/Badbird5907/Lightning
    //totally non-biased event bus choice and it definitely isn't my friend's
    implementation("net.badbird5907:Lightning:1.1.3-REL")

    // telegram bot api
    //https://github.com/rubenlagus/TelegramBots
    implementation("org.telegram:telegrambots:6.8.0")
    implementation("org.telegram:telegrambots-abilities:6.8.0")

    //compileOnly("org.projectlombok:lombok:1.18.26")
    //annotationProcessor("org.projectlombok:lombok:1.18.26")
}

group = "dev.digitaldragon"
version = "1.0-SNAPSHOT"
description = "WikiBot"
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
        archiveFileName.set("WikiBot-shadow.jar")
        manifest.attributes["Main-Class"] = "dev.digitaldragon.WikiBot"
    }
    javadoc {
        options.encoding = UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = UTF_8.name() // We want UTF-8 for everything
    }
}