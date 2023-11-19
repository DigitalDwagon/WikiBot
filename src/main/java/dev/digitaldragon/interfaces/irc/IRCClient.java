package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.irc.IrcCommandListener;
import dev.digitaldragon.util.EnvConfig;
import lombok.Getter;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.auth.GameSurge;

/**
 * IRCClient manages the bot's IRC connection.
 */
@Getter
public class IRCClient {
    private static Client client;
    private static boolean enabled = false;

    /**
     * Sends a message to the IRC channel defined in the config.
     *
     * @param message the message to send to the IRC channel
     */
    public static void sendMessage(String message) {
        if (!enabled) return;
        client.sendMessage(EnvConfig.getConfigs().get("ircchannel").trim(), message);
    }
    /**
     * Sends a message to the IRC channel defined in the config, directed at a specific user.
     *
     * @param user the user to direct the message to
     * @param message the message to send to the user in the IRC channel
     */
    public static void sendMessage(String user, String message) {
        sendMessage(user + ": " + message);
    }

    public static void enable() {
        enabled = true;
        connect();
    }

    /**
     * Reconnects the IRC client by shutting it down and then reconnecting.
     */
    public static void reconnect() {
        if (client == null) return;
        client.shutdown();
        // A bug in the IRC library causes the client connect function to not work properly.
        // To remedy this, we just rebuild the client from scratch.
        connect();
    }

    /**
     * Connects the IRC client to the specified server, with the provided configurations.
     */
    public static void connect() {
        if (!enabled) return;
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
