package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.JobStatus;
import dev.digitaldragon.util.Config;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.interactions.command.CommandImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public class DiscordClient {
    @Getter
    private JDA instance;
    @Getter
    private DiscordJobListener jobListener;

    public DiscordClient() {
        Logger logger = LoggerFactory.getLogger(DiscordClient.class);
        Config.DiscordConfig config = WikiBot.getConfig().getDiscordConfig();
        if (!config.isEnabled()) {
            logger.info("Discord module is disabled, skipping...");
            return;
        }
        enabled = true;

        GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS };
        try {
            instance = JDABuilder.create(config.token(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.VOICE_STATE)
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    //.addEventListeners(new DokuWikiDumperPlugin(), new TestingCommand(), new WikiTeam3Plugin())
                    .addEventListeners(new DiscordDokuWikiListener(), new DiscordMediaWikiListener(), new DiscordAdminListener(), new DiscordReuploadListener(), new DiscordButtonListener(), new DiscordCommandListener())
                    .build();
        } catch (LoginException loginException) {
            instance.shutdownNow();
            logger.error("####################");
            logger.error("Failed to log in to Discord. The Discord module will be disabled.", loginException);
            logger.error("####################");

            enabled = false;
            return;
        }

        instance.updateCommands().addCommands(
                Commands.slash("mediawiki_dump", "Dump a MediaWiki site with wikiteam3")
                        // We will only include the following common options here: URL, explain, API, Index, Images, XML, XMLApiExport, XMLRevisions, Delay, Force, BypassCDNImageCompression, and .
                        .addOption(OptionType.STRING, "url", "The URL of the wiki to dump", false)
                        .addOption(OptionType.STRING, "explain", "Note about job displayed in /status.", false)
                        .addOption(OptionType.STRING, "api", "The API URL of the wiki to dump", false)
                        .addOption(OptionType.STRING, "index", "The index URL of the wiki to dump", false)
                        .addOption(OptionType.BOOLEAN, "images", "Whether to download images", false)
                        .addOption(OptionType.BOOLEAN, "xml", "Whether to download XML", false)
                        .addOption(OptionType.BOOLEAN, "xmlapiexport", "Download XML via the MediaWiki API", false)
                        .addOption(OptionType.BOOLEAN, "xmlrevisions", "Download XML via the MediaWiki revisions API", false)
                        .addOption(OptionType.NUMBER, "delay", "Delay between requests", false)
                        .addOption(OptionType.BOOLEAN, "force", "Ignore recent dump protections", false)
                        .addOption(OptionType.BOOLEAN, "bypass-cdn-image-compression", "Bypass CDN image compression.", false)
                        .addOption(OptionType.STRING, "extra-args", "Passes these command-line arguments in addition to other provided values.", false),
                Commands.slash("dokuwiki_dump", "Dump a DokuWiki site with dokuwiki-dumper")
                // Only includes url, explain, auto, ignore-disposition-header-missing
                        .addOption(OptionType.STRING, "url", "The URL of the wiki to dump", false)
                        .addOption(OptionType.STRING, "explain", "Note about job displayed in /status.", false)
                        .addOption(OptionType.BOOLEAN, "auto", "Automatically follow redirects", false)
                        .addOption(OptionType.BOOLEAN, "ignore-disposition-missing", "Ignore missing Content-Disposition headers", false)
                        .addOption(OptionType.STRING, "extra-args", "Passes these command-line arguments in addition to other provided values.", false),
                Commands.slash("pukiwiki_archive", "Archive a PukiWiki site with pukiwiki-dumper")
                // only includes url, auto, ignore-action-disabled-edit, explain
                        .addOption(OptionType.STRING, "url", "The URL of the wiki to dump", false)
                        .addOption(OptionType.BOOLEAN, "auto", "Automatically follow redirects", false)
                        .addOption(OptionType.BOOLEAN, "ignore-action-disabled-edit", "Ignore disabled edit actions", false)
                        .addOption(OptionType.STRING, "explain", "Note about job displayed in /status.", false)
                        .addOption(OptionType.STRING, "extra-args", "Passes these command-line arguments in addition to other provided values.", false),
                Commands.slash("help", "Get help with using the bot"),
                Commands.slash("status", "Get the status of a job")
                        .addOption(OptionType.STRING, "job", "The ID of the job to get the status of", true),
                Commands.slash("abort", "Abort a job")
                        .addOption(OptionType.STRING, "job", "The ID of the job to abort", true)

        ).queue();

        jobListener = new DiscordJobListener();
        WikiBot.getBus().register(jobListener);

    }

    @Getter
    public static boolean enabled = false;

    public static void enable() {
        enabled = true;
        WikiBot.getBus().register(new DiscordJobListener());
    }


    public static EmbedBuilder getStatusEmbed(Job job) {

        EmbedBuilder builder = new EmbedBuilder();
        JobMeta meta = job.getMeta();

        builder.setTitle(meta.getTargetUrl().orElse("Job"), meta.getTargetUrl().orElse(null));

        builder.addField("User", meta.getUserName(), true);
        builder.addField("Job ID", "`" +  job.getId() + "`", true);
        builder.addField("Type", job.getType().name(), true);
        String quickLinks = "";

        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING) {
            //
        }
        else if (job.getStatus() == JobStatus.FAILED || job.getStatus() == JobStatus.ABORTED) {
            builder.setDescription("<:failed:1214681282626326528> Failed");
            builder.setColor(Color.RED);
            builder.addField("Failed Task", String.format("`%s` (Exit Code `%s`)", job.getRunningTask(), job.getFailedTaskCode()), true);
            quickLinks += "[Logs](" + job.getLogsUrl() + ") ";
        }
        else if (job.getStatus() == JobStatus.COMPLETED) {
            builder.setDescription("<:done:1214681284778000504> Done!");
            builder.setColor(Color.GREEN);
            quickLinks += "[Logs](" + job.getLogsUrl() + ") ";
            if (job.getArchiveUrl() != null) {
                quickLinks += "[Archive](" + job.getArchiveUrl() + ") ";
            }
        }

        if (!quickLinks.isEmpty()) {
            builder.addField("Quick Links", quickLinks, true);
        }


        switch (job.getStatus()) {
            case QUEUED:
                builder.setDescription("<:inprogress:1214681283771375706> In queue...");
                builder.setColor(Color.YELLOW);
                break;
            case RUNNING:
                builder.setDescription("<:inprogress:1214681283771375706> Running...");
                builder.setColor(Color.YELLOW);
                builder.addField("Task", job.getRunningTask() == null ? "Unknown" : job.getRunningTask(), true);
                break;
            case FAILED:
                builder.setDescription("<:failed:1214681282626326528> Failed");
                builder.setColor(Color.RED);
                break;
            case ABORTED:
                builder.setDescription("<:failed:1214681282626326528> Aborted");
                builder.setColor(Color.ORANGE);
                break;
            case COMPLETED:
                builder.setDescription("<:done:1214681284778000504> Done!");
                builder.setColor(Color.GREEN);
                break;
        }
        if (meta.getExplain().isPresent()) {
            builder.addField("Explanation", meta.getExplain().get(), false);
        }
        return builder;
    }


    public static List<ItemComponent> getJobActionRow(Job job) {
        return List.of(getAbortButton(job), getStatusButton(job), getLogsButton(job), getArchiveButton(job));
    }

    public static Button getAbortButton(Job job) {
        return Button.danger("abort_" + job.getId(), "Abort")
                .withEmoji(Emoji.fromUnicode("✖"))
                .withDisabled(!(job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING));
    }

    public static Button getStatusButton(Job job) {
        return Button.secondary("status_" + job.getId(), "Info")
                .withEmoji(Emoji.fromUnicode("ℹ️"));
    }

    public static Button getLogsButton(Job job) {
        if (job.getLogsUrl() == null) {
            return Button.secondary("logs_" + job.getId(), "Logs")
                    //.withUrl("about:blank")
                    .withEmoji(Emoji.fromUnicode("📄"))
                    .withDisabled(true);
        }
        return Button.secondary("logs_" + job.getId(), "Logs")
                .withEmoji(Emoji.fromUnicode("📄"))
                .withUrl(job.getLogsUrl());
    }

    public static Button getArchiveButton(Job job) {
        if (job.getArchiveUrl() == null) {
            return Button.secondary("archive_" + job.getId(), "Archive")
                    //.withUrl("about:blank")
                    .withEmoji(Emoji.fromUnicode("📁"))
                    .withDisabled(true);
        }
        return Button.secondary("archive_" + job.getId(), "Archive")
                .withEmoji(Emoji.fromUnicode("📁"))
                .withUrl(job.getArchiveUrl());
    }




}
