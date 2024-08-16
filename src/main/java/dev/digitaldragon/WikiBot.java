package dev.digitaldragon;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import dev.digitaldragon.db.SqliteManager;
import dev.digitaldragon.interfaces.api.JavalinAPI;
import dev.digitaldragon.interfaces.discord.DiscordClient;
import dev.digitaldragon.interfaces.irc.IRCClient;
import dev.digitaldragon.interfaces.telegram.TelegramClient;
import dev.digitaldragon.jobs.CleanupListener;
import dev.digitaldragon.jobs.LogFiles;
import dev.digitaldragon.util.Config;
import dev.digitaldragon.warcs.WarcproxManager;
import lombok.Getter;
import net.badbird5907.lightning.EventBus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WikiBot {
    @Getter
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);
    @Getter
    public static EventBus bus = new EventBus();
    public static final GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS };
    @Getter
    private static Config config = null;
    @Getter
    private static DiscordClient discordClient = null;
    @Getter
    private static LogFiles logFiles = new LogFiles();
    public static String getVersion() {
        return "1.5.2";
    }

    public static void main (String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            WarcproxManager.stopCleanly();
        }));
        Logger logger = LoggerFactory.getLogger(WikiBot.class);
        try {
            config = new Config("config.json");
        } catch (IOException e) {
            logger.error("Failed to load config", e);
            System.exit(1);
        }
        discordClient = new DiscordClient();

        WarcproxManager.run();
        IRCClient.enable();
        DiscordClient.enable();
        TelegramClient.enable();
        logFiles = new LogFiles();
        bus.register(logFiles);
        bus.register(new CleanupListener());
        SqliteManager manager = new SqliteManager();
        bus.register(manager);
        manager.load();

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get("cdn.digitaldragon.dev");
        System.out.println(bucket.getName());

        JavalinAPI.register();
    }
}