package dev.digitaldragon;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.discord.DiscordAdminListener;
import dev.digitaldragon.interfaces.discord.DiscordDokuWikiListener;
import dev.digitaldragon.interfaces.discord.DiscordMediaWikiListener;
import dev.digitaldragon.interfaces.discord.DiscordReuploadListener;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.util.EnvConfig;
import dev.digitaldragon.util.IRCClient;
import dev.digitaldragon.warcs.WarcproxManager;
import dev.digitaldragon.web.DashboardWebsocket;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Spark;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static spark.Spark.*;

public class WikiBot {
    @Getter
    public static JDA instance;
    @Getter
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);
    public static final GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS };


    public static void main (String[] args) throws LoginException, InterruptedException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            WarcproxManager.stopCleanly();
        }));
        WarcproxManager.run();
        instance = JDABuilder.create(EnvConfig.getConfigs().get("token"), Arrays.asList(INTENTS))
                .enableCache(CacheFlag.VOICE_STATE)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                //.addEventListeners(new DokuWikiDumperPlugin(), new TestingCommand(), new WikiTeam3Plugin())
                .addEventListeners(new DiscordDokuWikiListener(), new DiscordMediaWikiListener(), new DiscordAdminListener(), new DiscordReuploadListener())
                .build();

        IRCClient.connect();
        instance.awaitReady();

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get("cdn.digitaldragon.dev");
        System.out.println(bucket.getName());

        Guild testServer = instance.getGuildById(EnvConfig.getConfigs().get("discord_server").trim());
        if (testServer != null) {

            // ----------------------------- dokuwiki ----------------------------- //

            SubcommandData dokuBulk = new SubcommandData("bulk", "Archive DokuWikis in bulk.")
                    .addOption(OptionType.ATTACHMENT, "file", ".txt file of wikis to archive, separated by newline. Text after URL treated as note.", true);

            SubcommandData dokuSingle = new SubcommandData("single", "Archive a single DokuWiki")
                    .addOption(OptionType.STRING, "url", "doku.php url for the wiki you want to archive", true)
                    .addOption(OptionType.STRING, "explain", "Reason why the wiki is being dumped. For ops and your ease keeping track", true);


            List<SubcommandData> dokuCommands = new ArrayList<SubcommandData>();
            dokuCommands.add(dokuSingle);
            dokuCommands.add(dokuBulk);

            for (SubcommandData data : dokuCommands) {            //We currently disallow i-love-retro trim-php-warnings --parser --username --password --cookies --g as options.
                data.addOption(OptionType.BOOLEAN, "ignore_disposition", "Ignore missing disposition header. Useful for old DokuWiki versions (default off)", false)
                        .addOption(OptionType.INTEGER, "delay", "Delay between requests (default 0)", false)
                        .addOption(OptionType.INTEGER, "retry", "Maximum number of retries (default 5)", false)
                        .addOption(OptionType.INTEGER, "hard_retry", "Maximum number of retries for hard errors (default 3)", false)
                        .addOption(OptionType.BOOLEAN, "current_only", "Only dump the latest revision, no history. (default off)", false)
                        .addOption(OptionType.INTEGER, "threads", "Worker threads to dump with (default 3)", false)
                        .addOption(OptionType.BOOLEAN, "auto", "Dump content, media, html, ignore disabled edit, with 3 threads. (default on)", false)
                        .addOption(OptionType.BOOLEAN, "no_resume", "Don't resume a previous dump. (default off)", false)
                        .addOption(OptionType.BOOLEAN, "insecure", "Disable SSL certificate validation.", false)
                        .addOption(OptionType.BOOLEAN, "ignore_errors", "DANGEROUS: Ignore errors. May cause incomplete dumps. (default off)", false)
                        .addOption(OptionType.BOOLEAN, "ignore_disabled_edit", "Ignore editing disabled. May cause partial dumps. (default off)", false)
                        .addOption(OptionType.BOOLEAN, "upload", "Automatically upload to IA. Requires an operator to upload otherwise. (default on)", false)
                        .addOption(OptionType.BOOLEAN, "content", "Dump content? (default: auto)", false)
                        .addOption(OptionType.BOOLEAN, "media", "Dump media? (default: auto)", false)
                        .addOption(OptionType.BOOLEAN, "html", "Dump html? (default: auto)", false)
                        .addOption(OptionType.BOOLEAN, "pdf", "Dump pdf? (default: auto)", false);
            }

            testServer.upsertCommand("dokuwikiarchive", "Archive a DokuWiki using DokuWikiArchiver and upload to archive.org")
                    .addSubcommands(dokuSingle, dokuBulk)
                    .queue();

            //testServer.upsertCommand("testarchivetask", "A testing archive task. Archives absolutely nothing.").queue();


            // ----------------------------- Mediawiki ----------------------------- //
            SubcommandData mediaBulk = new SubcommandData("bulk", "Archive MediaWikis in bulk. Note: Does not use launcher, jobs run in parallel")
                    .addOption(OptionType.ATTACHMENT, "file", ".txt file of wikis to archive, separated by newline. Text after URL is treated as note.", true);

            SubcommandData mediaSingle = new SubcommandData("single", "Archive a single MediaWiki")
                    .addOption(OptionType.STRING, "explain", "Reason why the wiki is being dumped. For ops and your ease keeping track", true)
                    .addOption(OptionType.STRING, "url", "url for the wiki you want to archive", false);



            List<SubcommandData> mediaCommands = new ArrayList<SubcommandData>();
            mediaCommands.add(mediaSingle);
            mediaCommands.add(mediaBulk);

            for (SubcommandData data : mediaCommands) {            //We currently disallow --help --version --cookies --path --resume --force --user --pass --http-user --http-pass --xmlrevisions_page --namespaces --exnamespaces --stdout-log-file
                data.addOption(OptionType.STRING, "index", "Index url for the wiki you want to archive", false)
                        .addOption(OptionType.STRING, "api", "API url for the wiki you want to archive", false)
                        .addOption(OptionType.BOOLEAN, "insecure", "Disable SSL certificate validation.", false)
                        .addOption(OptionType.BOOLEAN, "xml", "Dump XML? (default: on)", false)
                        .addOption(OptionType.BOOLEAN, "images", "Dump images? (default: on)", false)
                        .addOption(OptionType.BOOLEAN, "bypass_compression", "Bypass CDN image compression (eg Cloudflare Polish)", false)
                        .addOption(OptionType.BOOLEAN, "xml_api_export", "Export XML dump using API:revisions instead of Special:Export (requires current_only, default: off)", false)
                        .addOption(OptionType.BOOLEAN, "xml_revisions", "Export all revisions from API:Allrevisions. MW 1.27+ Only (no current_only, default: off)", false)
                        .addOption(OptionType.NUMBER, "delay", "Delay between requests (0-200, default 5)", false)
                        .addOption(OptionType.INTEGER, "retry", "Maximum number of retries (0-50, default 5)", false)
                        .addOption(OptionType.BOOLEAN, "current_only", "Only dump the latest revision, no history. (default off)", false)
                        .addOption(OptionType.INTEGER, "api_chunksize", "Chunk size for MediaWiki API requests (1-500, default 50)", false)
                        .addOption(OptionType.BOOLEAN, "force", "Force download, even when there is a recent dump on IA (default off)", false)
                        .addOption(OptionType.BOOLEAN, "disable_image_verification", "Disable verification of the image size and hash after it's downloaded (default off)", false)
                        .addOption(OptionType.BOOLEAN, "old_backend", "Run jobs through the legacy backend. WARNING: DISABLES /STATUS AND /ABORT. (default off)", false);
            }

            testServer.upsertCommand("mediawikiarchive", "Archive a MediaWiki using WikiTeam3 (mediawiki-scraper) and upload to archive.org")
                    .addSubcommands(mediaSingle, mediaBulk)
                    .queue();

            testServer.upsertCommand("poke", "Poke the bot to reconnect to IRC").queue();

            testServer.upsertCommand("reupload", "Reupload a failed upload to the Internet Archive")
                    .addOption(OptionType.STRING, "jobid", "Job ID of the failed upload", true)
                    .queue();
        }
        // WebSocket route definition (as mentioned in previous response).
        webSocket("/api/logfirehose", DashboardWebsocket.class);
        Spark.port(4567);
        enableCORS("*", "*", "*");
        // API endpoint to retrieve job information by ID.
        get("/api/jobs/:id", (req, res) -> {
            String jobId = req.params(":id");
            Job job = JobManager.get(jobId);
            if (job != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("jobId", jobId);
                jsonObject.put("status", job.getStatus());
                jsonObject.put("explanation", job.getExplanation());
                jsonObject.put("user", job.getUserName());
                jsonObject.put("started", job.getStartTime());
                jsonObject.put("name", job.getName());
                jsonObject.put("runningTask", job.getRunningTask());
                //jsonObject.put("directory", job.getDirectory());
                jsonObject.put("failedTaskCode", job.getFailedTaskCode());
                jsonObject.put("threadChannel", job.getThreadChannel().getId());
                jsonObject.put("archiveUrl", job.getArchiveUrl());
                jsonObject.put("type", job.getType());
                jsonObject.put("isRunning", job.isRunning());
                jsonObject.put("allTasks", job.getAllTasks());
                jsonObject.put("logsUrl", job.getLogsUrl());


                res.status(200);
                return jsonObject.toString();
            } else {
                res.status(404);
                return "Job not found";
            }
        });

        get("/api/jobs", (req, res) -> {
            JSONObject jsonObject = new JSONObject();
            JSONArray runningJobs = new JSONArray();
            for (Job job : JobManager.getActiveJobs()) {
                runningJobs.put(job.getId());
            }
            jsonObject.put("running", runningJobs);

            JSONArray queuedJobs = new JSONArray();
            for (Job job : JobManager.getQueuedJobs()) {
                queuedJobs.put(job.getId());
            }
            jsonObject.put("queued", queuedJobs);
            res.status(200);
            return jsonObject.toString();
        });


    }

    public static TextChannel getLogsChannel() {
        Guild testServer = WikiBot.getInstance().getGuildById(EnvConfig.getConfigs().get("discord_server").trim());
        if (testServer == null) {
            return null;
        }
        TextChannel channel = (TextChannel) testServer.getGuildChannelById(EnvConfig.getConfigs().get("discord_channel").trim());
        return channel;
    }

    public static TextChannel getLogsChannelSafely() throws UserErrorException {
        TextChannel discordChannel = WikiBot.getLogsChannel();
        if (discordChannel == null) {
            throw new UserErrorException("Something went wrong.");
        }
        return discordChannel;
    }


    private static void enableCORS(final String origin, final String methods, final String headers) {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            response.type("application/json");
        });
    }

}