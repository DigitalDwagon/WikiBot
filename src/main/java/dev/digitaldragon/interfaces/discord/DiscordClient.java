package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.WikiBot;
import lombok.Getter;

public class DiscordClient {
    @Getter
    public static boolean enabled = false;

    public static void enable() {
        enabled = true;
        WikiBot.getBus().register(new DiscordJobListener());
    }
}
