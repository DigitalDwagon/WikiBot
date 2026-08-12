package dev.digitaldragon.util;

import dev.digitaldragon.WikiBot;
import lombok.Getter;

import java.util.Scanner;

@Getter
public class BuildInfo {
    public String version;

    public static BuildInfo load() {
        // Stupid Scanner trick: https://stackoverflow.com/questions/6068197/read-resource-text-file-to-string-in-java
        String buildInfoJson = new Scanner(UserAgentParser.class.getResourceAsStream("/build-info.json"), "UTF-8").useDelimiter("\\A").next();
        return WikiBot.getGson().fromJson(buildInfoJson, BuildInfo.class);
    }
}
