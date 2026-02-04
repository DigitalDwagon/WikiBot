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
import dev.digitaldragon.jobs.queues.QueueManager;
import dev.digitaldragon.util.Config;
import dev.digitaldragon.util.FileTypeAdapter;
import dev.digitaldragon.util.InstantTypeAdapter;
import dev.digitaldragon.util.OptionalSerializer;
import lombok.Getter;
import net.badbird5907.lightning.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WikiBot {
    @Getter
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);
    @Getter
    public static EventBus bus = new EventBus();
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
        return "1.9.1";
    }

    @Getter
    private static File scriptDirectory = null;
    @Getter
    private static QueueManager queueManager = null;

    public static void main (String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            JobManager.getJobs().forEach((job) -> {
                sqliteManager.saveJob(job);
            });
        }));
        Logger logger = LoggerFactory.getLogger(WikiBot.class);
        try {
            File configFile = new File("config.json");
            String json = Files.readString(configFile.toPath());
            config = gson.fromJson(json, Config.class);
        } catch (IOException e) {
            logger.error("Failed to load config", e);
            System.exit(1);
        }

        scriptDirectory = new File("wikibot-scripts");
        if (!scriptDirectory.exists()) {
            logger.error("Failed to find script directory");
            System.exit(1);
        }

        queueManager = new QueueManager();

        discordClient = new DiscordClient();

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
