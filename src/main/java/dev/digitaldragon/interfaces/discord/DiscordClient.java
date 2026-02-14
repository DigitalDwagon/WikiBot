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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;

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

        GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EXPRESSIONS };
        instance = JDABuilder.create(config.token(), Arrays.asList(INTENTS))
                .enableCache(CacheFlag.VOICE_STATE)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                //.addEventListeners(new DokuWikiDumperPlugin(), new TestingCommand(), new WikiTeam3Plugin())
                .addEventListeners(new DiscordAdminListener(), new DiscordButtonListener(), new DiscordCommandListener())
                .build();

        try {
            instance.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
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

        Guild mainGuild = instance.getGuildById(349920496550281226L);
        if (mainGuild != null) {
            mainGuild.upsertCommand("poke_irc", "Causes the bot to disconnect and reconnect from IRC.").queue();
            System.out.println("Registering poke_irc command for the main guild...");
        }
        System.out.println(mainGuild);
        System.out.println(instance.getSelfUser().getId());


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

        if (job.isRunning() && job.getRunningTask() != null) {
            builder.addField("Task", job.getRunningTask(), true);
        }

        if (job.getFailedTaskCode() != 0) {
            builder.addField("Failed Task", String.format("`%s` (Exit Code `%s`)", job.getRunningTask(), job.getFailedTaskCode()), true);
        }

        Map<JobStatus, String> statusEmojis = Map.of(
                JobStatus.QUEUED, "<:inprogress:1214681283771375706>",
                JobStatus.RUNNING, "<:inprogress:1214681283771375706>",
                JobStatus.FAILED, "<:failed:1214681282626326528>",
                JobStatus.ABORTED, "<:failed:1214681282626326528>",
                JobStatus.COMPLETED, "<:done:1214681284778000504>"
                );
        Map<JobStatus, Color> statusColors = Map.of(
                JobStatus.QUEUED, Color.YELLOW,
                JobStatus.RUNNING, Color.YELLOW,
                JobStatus.FAILED, Color.RED,
                JobStatus.ABORTED, Color.ORANGE,
                JobStatus.COMPLETED, Color.GREEN
        );

        JobStatus status = job.getStatus();
        builder.setColor(statusColors.get(status));
        builder.setDescription(statusEmojis.get(status) + " " + status.toString().charAt(0) + status.toString().substring(1).toLowerCase());

        if (meta.getExplain().isPresent()) {
            builder.addField("Explanation", meta.getExplain().get(), false);
        }

        return builder;
    }


    public static List<ItemComponent> getJobActionRow(Job job) {
        List<ItemComponent> buttons = new ArrayList<>();

        String id = job.getId();

        if (job.isRunning()) {
            buttons.add(Button.danger("abort_" + id, "Abort")
                            .withEmoji(Emoji.fromUnicode("✖")));
        }

        buttons.add(Button.secondary("status_" + job.getId(), "Info")
                .withEmoji(Emoji.fromUnicode("ℹ️")));

        if (job.getLogsUrl() != null) {
            buttons.add(Button.link("logs_" + job.getId(), "Logs")
                    .withEmoji(Emoji.fromUnicode("📄"))
                    .withUrl(job.getLogsUrl()));
        }

        if (job.getArchiveUrl() != null) {
            buttons.add(Button.secondary("archive_" + job.getId(), "Archive")
                    .withEmoji(Emoji.fromUnicode("📁"))
                    .withUrl(job.getArchiveUrl()));
        }

        return buttons;
    }

    public Optional<User> getUserById(String id) {
        //loop through all guilds to check for the user
        for (var guild : instance.getGuilds()) {
            try {
                User user = guild.retrieveMemberById(id).complete().getUser();
                if (user != null) {
                    return Optional.of(user);
                }
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(DiscordClient.class).error("Failed to get user by ID", e);
            }
        }
        return Optional.empty();
    }




}
