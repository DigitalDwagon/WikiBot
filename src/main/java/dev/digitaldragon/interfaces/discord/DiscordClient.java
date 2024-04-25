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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class DiscordClient {
    @Getter
    private JDA instance;
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
                    .addEventListeners(new DiscordDokuWikiListener(), new DiscordMediaWikiListener(), new DiscordAdminListener(), new DiscordReuploadListener(), new DiscordButtonListener())
                    .build();
        } catch (LoginException loginException) {
            instance.shutdownNow();
            logger.error("####################");
            logger.error("Failed to log in to Discord. The Discord module will be disabled.", loginException);
            logger.error("####################");

            enabled = false;
            return;
        }

        WikiBot.getBus().register(new DiscordJobListener());

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
                builder.addField("Task", job.getRunningTask(), true);
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
}
