package dev.digitaldragon.util;

import dev.digitaldragon.WikiBot;

public class IRCClient {
    public static void sendMessage(String message) {
        //if (WikiBot.getIrcClient().getChannels().isEmpty()) {
        //    WikiBot.getIrcClient().reconnect("Auto-detected issue. Reconnecting.");
        //}
        WikiBot.getIrcClient().sendMessage(EnvConfig.getConfigs().get("ircchannel").trim(), message);
    }
    public static void sendMessage(String user, String message) {
        sendMessage(user + ": " + message);
    }

    public static void reconnect() {
        WikiBot.getIrcClient().shutdown();
        WikiBot.getIrcClient().connect();
    }
}
