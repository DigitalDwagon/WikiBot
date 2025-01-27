package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.util.Config;
import lombok.Getter;
import org.kitteh.irc.client.library.Client;

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
        client.sendMessage(WikiBot.getConfig().getIrcConfig().channel(), message);
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
        WikiBot.getBus().register(new IRCJobListener());
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
        Config.IRCConfig config = WikiBot.getConfig().getIrcConfig();
        client = Client.builder()
                .nick(config.nick())
                .realName(config.realName())
                .user(config.nick())
                .server().host(config.server()).port(config.port()).secure(true).then()
                .buildAndConnect();

        client.getEventManager().registerEventListener(new IrcCommandListener());
        client.getEventManager().registerEventListener(new IRCBulkCommand());
        client.addChannel(config.channel());
    }
}
