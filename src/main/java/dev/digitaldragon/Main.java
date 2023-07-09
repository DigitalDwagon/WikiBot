package dev.digitaldragon;

import dev.digitaldragon.commands.ArchiveCommand;
import dev.digitaldragon.util.EnvConfig;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    @Getter
    public static JDA instance;
    @Getter
    public static ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static final GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS };


    public static void main (String[] args) throws LoginException, InterruptedException {
        instance = JDABuilder.create(EnvConfig.getConfigs().get("token"), Arrays.asList(INTENTS))
                .enableCache(CacheFlag.VOICE_STATE)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners(new ArchiveCommand())
                .build();


        instance.awaitReady();

        Guild testServer = instance.getGuildById("349920496550281226");
        if (testServer != null) {

            SubcommandData bulk = new SubcommandData("bulk", "Archive DokuWikis in bulk.")
                    .addOption(OptionType.ATTACHMENT, "file", ".txt file of wikis to archive, separated by newline. Text after URL treated as note.", true);

            SubcommandData single = new SubcommandData("single", "Archive a single DokuWiki")
                    .addOption(OptionType.STRING, "url", "doku.php url for the wiki you want to archive", true)
                    .addOption(OptionType.STRING, "explain", "Reason why the wiki is being dumped. For ops and your ease keeping track", true);


            List<SubcommandData> commands = new ArrayList<SubcommandData>();
            commands.add(single);
            commands.add(bulk);

            for (SubcommandData data : commands) {            //We currently disallow i-love-retro trim-php-warnings --parser --username --password --cookies --g as options.
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
                    .addSubcommands(single, bulk)
                    .queue();

        }

    }

}