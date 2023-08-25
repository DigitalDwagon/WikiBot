package dev.digitaldragon.util;

import dev.digitaldragon.commands.IrcCommandListener;
import lombok.Getter;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.auth.GameSurge;

public class IRCClient {
    @Getter
    private static Client client;

    public static void sendMessage(String message) {
        //if (WikiBot.getIrcClient().getChannels().isEmpty()) {
        //    WikiBot.getIrcClient().reconnect("Auto-detected issue. Reconnecting.");
        //}
        client.sendMessage(EnvConfig.getConfigs().get("ircchannel").trim(), message);
    }
    public static void sendMessage(String user, String message) {
        sendMessage(user + ": " + message);
    }

    public static void reconnect() {
        client.shutdown();
        connect();
    }

    public static void connect() {
        client = Client.builder()
                .nick(EnvConfig.getConfigs().get("ircnick").trim())
                .realName(EnvConfig.getConfigs().get("ircnick").trim())
                .user(EnvConfig.getConfigs().get("ircnick").trim())
                .server().host("irc.hackint.org").port(6697).secure(true).then()
                .buildAndConnect();

        client.getEventManager().registerEventListener(new IrcCommandListener());

        if (Boolean.parseBoolean(EnvConfig.getConfigs().get("irclogin"))) {
            client.getAuthManager().addProtocol(new GameSurge(client, EnvConfig.getConfigs().get("ircnick").trim(), EnvConfig.getConfigs().get("ircpass").trim()));
        }
        client.addChannel(EnvConfig.getConfigs().get("ircchannel").trim());
    }
}
