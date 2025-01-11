package dev.digitaldragon;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.digitaldragon.db.SqliteManager;
import dev.digitaldragon.interfaces.api.JavalinAPI;
import dev.digitaldragon.interfaces.discord.DiscordClient;
import dev.digitaldragon.interfaces.irc.IRCClient;
import dev.digitaldragon.interfaces.telegram.TelegramClient;
import dev.digitaldragon.jobs.CleanupListener;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.LogFiles;
import dev.digitaldragon.util.*;
import dev.digitaldragon.warcs.WarcproxManager;
import lombok.Getter;
import net.badbird5907.lightning.EventBus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
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
    private static SqliteManager sqliteManager = null;
    @Getter
    private static LogFiles logFiles = new LogFiles();
    @Getter
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(File.class, new FileTypeAdapter())
            .registerTypeAdapter(Optional.class, new OptionalSerializer<>())
            .create();
    public static String getVersion() {
        return "1.6.3";
    }

    public static void main (String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            WarcproxManager.stopCleanly();
            JobManager.getJobs().forEach((job) -> {
                sqliteManager.saveJob(job);
            });
        }));
        Logger logger = LoggerFactory.getLogger(WikiBot.class);
        try {
            config = new Config("config.json");
        } catch (IOException e) {
            logger.error("Failed to load config", e);
            System.exit(1);
        }
        discordClient = new DiscordClient();
        JobManager.setQueueConcurrency("default", 15);
        JobManager.setQueuePriority("default", 0);

        WarcproxManager.run();
        IRCClient.enable();
        DiscordClient.enable();
        TelegramClient.enable();
        logFiles = new LogFiles();
        bus.register(logFiles);
        bus.register(new CleanupListener());
        sqliteManager = new SqliteManager();
        bus.register(sqliteManager);
        sqliteManager.load();

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get("cdn.digitaldragon.dev");
        System.out.println(bucket.getName());

        JavalinAPI.register();
    }
}