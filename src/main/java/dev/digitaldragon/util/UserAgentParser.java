package dev.digitaldragon.util;

import com.beust.jcommander.IStringConverter;
import dev.digitaldragon.WikiBot;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class UserAgentParser implements IStringConverter<String> {
    @Override
    public String convert(String input) {

        // Stupid Scanner trick: https://stackoverflow.com/questions/6068197/read-resource-text-file-to-string-in-java
        String agentJson = new Scanner(UserAgentParser.class.getResourceAsStream("/user-agents.json"), "UTF-8").useDelimiter("\\A").next();
        List<UserAgent> agents = Arrays.asList(WikiBot.getGson().fromJson(agentJson, UserAgent[].class));

        return agents.stream()
                .filter(agent -> agent.getAliases().contains(input))
                .map(UserAgent::getName)
                .findFirst()
                .orElse(input);
    }

    @Getter
    private class UserAgent {
        List<String> aliases;
        String name;
    }
}
