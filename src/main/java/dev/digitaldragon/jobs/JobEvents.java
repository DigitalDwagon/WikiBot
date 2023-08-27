package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.util.EnvConfig;
import dev.digitaldragon.util.IRCClient;
import net.dv8tion.jda.api.entities.TextChannel;

public class JobEvents {
    public static void onJobFailure(Job job) {
        IRCClient.sendMessage(job.getUserName(), "Success! Job " + job.getId() + " completed successfully.");
        IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());

        TextChannel failChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_failure_channel"));
        if (failChannel != null)
            failChannel.sendMessage(String.format("%s for %s:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nArchive URL: %s\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getArchiveUrl(), job.getExplanation())).queue();

        job.getThreadChannel().sendMessage("Job ended.").queue();
        job.getThreadChannel().sendMessage("Explanation: ```" + job.getExplanation() + "```").queue();
        job.getThreadChannel().sendMessage("Logs are available at " + job.getLogsUrl()).queue();
        job.getThreadChannel().sendMessage("Task indicated as failed.").queue();
    }

    public static void onJobSuccess(Job job) {
        IRCClient.sendMessage(job.getUserName(), "Success! Job " + job.getId() + " completed successfully.");
        IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());

        TextChannel successChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_success_channel"));
        if (successChannel != null)
            successChannel.sendMessage(String.format("%s for %s:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nArchive URL: %s\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getArchiveUrl(), job.getExplanation())).queue();

        job.getThreadChannel().sendMessage("Job ended.").queue();
        job.getThreadChannel().sendMessage("Explanation: ```" + job.getExplanation() + "```").queue();
        job.getThreadChannel().sendMessage("Logs are available at " + job.getLogsUrl()).queue();
    }

    public static void onJobAbort(Job job) {
        IRCClient.sendMessage(job.getUserName(), "Your job " + job.getId() + " was aborted.");
    }

    public static void onJobQueued(Job job) {
        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");

    }

    public static void onJobLogLine(Job job, String log) {
        //log to discord
    }
}
