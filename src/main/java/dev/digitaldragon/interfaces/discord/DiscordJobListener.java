package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.irc.IRCClient;
import dev.digitaldragon.interfaces.irc.IRCFormat;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobStatus;
import dev.digitaldragon.jobs.events.*;
import net.badbird5907.lightning.annotation.EventHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DiscordJobListener {
    private static Map<String, ThreadChannel> jobChannels = new HashMap<>();
    private static Map<String, String> jobLogs = new HashMap<>();

    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        onJobEnd(event.getJob());
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        onJobEnd(event.getJob());
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        onJobEnd(event.getJob());
    }

    private void onJobEnd(Job job) {
        String message = switch (job.getStatus()) {
            case COMPLETED -> "Job ended.";
            case FAILED -> "Job failed.";
            case ABORTED -> "Job aborted.";
            default -> "";
        };

        sendLogs(jobLogs.getOrDefault(job.getId(), ""), job);
        jobChannels.get(job.getId())
                .sendMessage(message)
                .setEmbeds(DiscordClient.getStatusEmbed(job).build())
                .setActionRows(getJobActionRow(job))
                .queue();

        Optional<String> channelId = switch (job.getStatus()) {
            case COMPLETED -> WikiBot.getConfig().getDiscordConfig().successChannel();
            case FAILED, ABORTED -> WikiBot.getConfig().getDiscordConfig().failureChannel();
            default -> Optional.empty();
        };
        sendStatusToChannel(job, channelId);
    }

    private void sendStatusToChannel(Job job, Optional<String> channelId) {
        if (channelId.isEmpty()) return;
        TextChannel channel = WikiBot.getDiscordClient().getInstance().getTextChannelById(channelId.get());
        if (channel == null) return;
        channel.sendMessageEmbeds(DiscordClient.getStatusEmbed(job).build()).queue();
    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        // <:done:1214681284778000504><:inprogress:1214681283771375706><:failed:1214681282626326528>
        Job job = event.getJob();
        JDA instance = WikiBot.getDiscordClient().getInstance();
        TextChannel channel = instance.getTextChannelById(WikiBot.getConfig().getDiscordConfig().channelId());
        if (channel == null) {
            LoggerFactory.getLogger(DiscordJobListener.class).error("Failed to access Discord channel " + WikiBot.getConfig().getDiscordConfig().channelId());
            return;
        }

        channel.createThreadChannel(truncateString(job.getName(), 100))
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    thread.sendMessageEmbeds(DiscordClient.getStatusEmbed(job).build())
                            .setActionRows(getJobActionRow(job))
                            .queue(sentMessage -> sentMessage.pin().queue());
                    jobChannels.put(job.getId(), thread);
                });

    }

    @EventHandler
    public synchronized void onJobLog(JobLogEvent event) {
        Job job = event.getJob();
        String message = event.getMessage();
        String logs = jobLogs.getOrDefault(job.getId(), "");
        if (logs.length() + message.length() > Message.MAX_CONTENT_LENGTH - 6) {
            sendLogs(logs, job);
            logs = jobLogs.getOrDefault(job.getId(), "");
        }
        message = truncateString(message, Message.MAX_CONTENT_LENGTH - 6);
        logs += message + "\n";
        jobLogs.put(job.getId(), logs);
    }

    private synchronized void sendLogs(String message, Job job) {
        if (message.isEmpty()) return;
        message = truncateString(message, Message.MAX_CONTENT_LENGTH - 6);
        try {
            jobChannels.get(job.getId()).sendMessage("```" + message + "```").queue();
            jobLogs.put(job.getId(), "");
        } catch (Exception e) {
            LoggerFactory.getLogger(DiscordJobListener.class).error("Failed to send logs to Discord channel for job " + job.getId(), e);
        }

    }


    private static ActionRow getJobActionRow(Job job) {
        return ActionRow.of(getAbortButton(job), getStatusButton(job), getLogsButton(job), getArchiveButton(job));
    }

    private static Button getAbortButton(Job job) {
        return Button.danger("abort_" + job.getId(), "Abort")
                .withEmoji(Emoji.fromUnicode("✖"))
                .withDisabled(!(job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING));
    }

    private static Button getStatusButton(Job job) {
        return Button.secondary("status_" + job.getId(), "Info")
                .withEmoji(Emoji.fromUnicode("ℹ️"));
    }

    private static Button getLogsButton(Job job) {
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

    private static Button getArchiveButton(Job job) {
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



    private static String truncateString(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        } else {
            return message.substring(0, maxLength - 3) + "...";
        }
    }
}
