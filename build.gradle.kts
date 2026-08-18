import kotlin.text.Charsets.UTF_8

java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.3"
    id("io.freefair.lombok") version "8.6"
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(platform("com.google.cloud:libraries-bom:26.62.0"))
    implementation("com.google.cloud:google-cloud-storage:2.50.0")
    implementation("net.dv8tion:JDA:5.3.2")
    // https://mvnrepository.com/artifact/org.kitteh.irc/client-lib
    implementation("org.kitteh.irc:client-lib:9.0.0")
    implementation("com.google.code.gson:gson:2.13.1")
    // https://mvnrepository.com/artifact/commons-io/commons-io
    //implementation("commons-io:commons-io:2.13.0")

    //used for XML parsing in link extractor.
    implementation("org.jsoup:jsoup:1.21.2")

    //used to decompress zstd streams
    // https://mvnrepository.com/artifact/com.github.luben/zstd-jni
    implementation("com.github.luben:zstd-jni:1.5.6-9")

    //web library for dashboard
    implementation("io.javalin:javalin:6.7.0")

    // https://mvnrepository.com/artifact/com.beust/jcommander
    implementation("com.beust:jcommander:1.82")

    //https://github.com/Badbird5907/Lightning
    //totally non-biased event bus choice and it definitely isn't my friend's
    implementation("net.badbird5907:Lightning:1.1.3-REL")

    // telegram bot api
    //https://github.com/rubenlagus/TelegramBots
    implementation("org.telegram:telegrambots:6.9.7.1")
    implementation("org.telegram:telegrambots-abilities:6.9.7.1")

    //loggers
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.16")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:2.0.12")

    // sqlite for database
    // https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    implementation("org.xerial:sqlite-jdbc:3.48.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    //compileOnly("org.projectlombok:lombok:1.18.26")
    //annotationProcessor("org.projectlombok:lombok:1.18.26")
}

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
        options.release.set(21)
    }
    shadowJar {
        archiveFileName.set("WikiBot-$version-shadow.jar")
        manifest.attributes["Main-Class"] = "dev.digitaldragon.WikiBot"
    }
    javadoc {
        options.encoding = UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = UTF_8.name() // We want UTF-8 for everything
        val props = mapOf("version" to project.version)
        inputs.properties(props) // re-run this task (don't cache) when the version changes

        filesMatching("user-agents.json") {
            expand(props)
        }
        filesMatching("build-info.json") {
            expand(props)
        }
    }
    test {
        useJUnitPlatform()
    }
}
