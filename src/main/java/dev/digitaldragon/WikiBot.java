package dev.digitaldragon;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import dev.digitaldragon.commands.DiscordDokuWikiListener;
import dev.digitaldragon.commands.DiscordMediaWikiListener;
import dev.digitaldragon.commands.IrcCommandListener;
import dev.digitaldragon.util.EnvConfig;
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
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.auth.GameSurge;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WikiBot {
    @Getter
    public static JDA instance;
    @Getter
    public static Client ircClient;
    @Getter
    public static ExecutorService executorService = Executors.newFixedThreadPool(5);
    public static final GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS };


    public static void main (String[] args) throws LoginException, InterruptedException, IOException {
        instance = JDABuilder.create(EnvConfig.getConfigs().get("token"), Arrays.asList(INTENTS))
                .enableCache(CacheFlag.VOICE_STATE)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                //.addEventListeners(new DokuWikiDumperPlugin(), new TestingCommand(), new WikiTeam3Plugin())
                .addEventListeners(new DiscordDokuWikiListener(), new DiscordMediaWikiListener())
                .build();

        ircClient = Client.builder()
                .nick(EnvConfig.getConfigs().get("ircnick").trim())
                .realName(EnvConfig.getConfigs().get("ircnick").trim())
                .user(EnvConfig.getConfigs().get("ircnick").trim())
                .server().host("irc.hackint.org").port(6697).secure(true).then()
                .buildAndConnect();

        ircClient.getEventManager().registerEventListener(new IrcCommandListener());

        if (Boolean.parseBoolean(EnvConfig.getConfigs().get("irclogin"))) {
            ircClient.getAuthManager().addProtocol(new GameSurge(ircClient, EnvConfig.getConfigs().get("ircnick").trim(), EnvConfig.getConfigs().get("ircpass").trim()));
        }
        ircClient.addChannel(EnvConfig.getConfigs().get("ircchannel").trim());


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
                        .addOption(OptionType.INTEGER, "delay", "Delay between requests (0-200, default 5)", false)
                        .addOption(OptionType.INTEGER, "retry", "Maximum number of retries (0-50, default 5)", false)
                        .addOption(OptionType.BOOLEAN, "current_only", "Only dump the latest revision, no history. (default off)", false)
                        .addOption(OptionType.INTEGER, "api_chunksize", "Chunk size for MediaWiki API requests (1-500, default 50)", false);
            }

            testServer.upsertCommand("mediawikiarchive", "Archive a MediaWiki using WikiTeam3 (mediawiki-scraper) and upload to archive.org")
                    .addSubcommands(mediaSingle, mediaBulk)
                    .queue();
        }
    }

    public static TextChannel getLogsChannel() {
        Guild testServer = WikiBot.getInstance().getGuildById(EnvConfig.getConfigs().get("discord_server").trim());
        if (testServer == null) {
            return null;
        }
        TextChannel channel = (TextChannel) testServer.getGuildChannelById(EnvConfig.getConfigs().get("discord_channel").trim());
        return channel;
    }

}