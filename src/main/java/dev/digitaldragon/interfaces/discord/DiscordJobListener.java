package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.events.*;
import net.badbird5907.lightning.annotation.EventHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DiscordJobListener {
    private static Map<String, ThreadChannel> jobChannels = new HashMap<>();
    private static Map<String, String> jobLogs = new HashMap<>();
    private static final Map<String, MessageChannel> jobUpdateChannels = new HashMap<>();

    public void setJobChannel(String jobId, MessageChannel channel) {
        // TODO: I'm not super happy with this method being here specifically
        jobUpdateChannels.put(jobId, channel);
    }

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
        JobMeta meta = job.getMeta();
        String message = switch (job.getStatus()) {
            case COMPLETED -> "Job ended.";
            case FAILED -> "Job failed.";
            case ABORTED -> "Job aborted.";
            default -> "";
        };

        sendLogs(jobLogs.getOrDefault(job.getId(), ""), job);

        jobChannels.get(job.getId())
                .sendMessage(message)
                .setActionRow(DiscordClient.getJobActionRow(job))
                .setEmbeds(DiscordClient.getStatusEmbed(job).build())
                .queue();

        List<MessageChannel> channels = new ArrayList<>();

        if (meta.getDiscordUserId().isPresent()) {
            message += " <@" + meta.getDiscordUserId().get() + ">"; // haha ping go brr
            Optional<User> user = WikiBot.getDiscordClient().getUserById(meta.getDiscordUserId().get());
            try {
                user.ifPresent(value -> channels.add(value.openPrivateChannel().complete()));
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(DiscordJobListener.class).error("Failed to open private channel with user " + meta.getDiscordUserId().get(), e);

            }

        }

        if (jobUpdateChannels.containsKey(job.getId())) {
            System.out.println("Sending to " + jobUpdateChannels.get(job.getId()).getId());
            channels.add(jobUpdateChannels.get(job.getId()));
        }



        for (MessageChannel channel : channels) {
            channel.sendMessage(message)
                    .setActionRow(DiscordClient.getJobActionRow(job))
                    .setEmbeds(DiscordClient.getStatusEmbed(job).build())
                    .queue();

        }

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

        channel.createThreadChannel(truncateString(job.getMeta().getTargetUrl().orElse(job.getId()), 100))
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    thread.sendMessageEmbeds(DiscordClient.getStatusEmbed(job).build())
                            .setActionRow(DiscordClient.getJobActionRow(job))
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


    private static String truncateString(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        } else {
            return message.substring(0, maxLength - 3) + "...";
        }
    }
}
