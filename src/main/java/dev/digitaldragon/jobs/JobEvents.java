package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.util.EnvConfig;
import dev.digitaldragon.util.IRCClient;
import net.dv8tion.jda.api.entities.TextChannel;

public class JobEvents {
    public static void onJobFailure(Job job) { //This method is called when a job fails (due to an improper task exit code, etc, as dictated by the job). The runningTask is the task that failed
        IRCClient.sendMessage(job.getUserName(), "Job " + job.getId() + " failed with exit code " + job.getFailedTaskCode() + ".");
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());

        TextChannel failChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_failure_channel"));
        if (failChannel != null)
            failChannel.sendMessage(String.format("%s for %s:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nFailed Task: %s\nExit code: `%s`\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getRunningTask(), job.getFailedTaskCode(), job.getExplanation())).queue();

        job.getThreadChannel().sendMessage("Job " + job.getId() + " failed with exit code " + job.getFailedTaskCode() + ".").queue();
        job.getThreadChannel().sendMessage("Explanation: ```" + job.getExplanation() + "```").queue();
        job.getThreadChannel().sendMessage("Logs are available at " + job.getLogsUrl()).queue();
        job.getThreadChannel().sendMessage("Task indicated as failed.").queue();
    }

    public static void onJobSuccess(Job job) { //This method is called when a job succeeds.
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

    public static void onJobAbort(Job job) { //This method is called when a job fails because it was aborted while running.
        IRCClient.sendMessage(job.getUserName(), "Your job " + job.getId() + " was aborted.");
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());

        TextChannel failChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_failure_channel"));
        if (failChannel != null)
            failChannel.sendMessage(String.format("%s for %s was aborted:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nFailed Task: %s\nExit code: `%s`\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getRunningTask(), job.getFailedTaskCode(), job.getExplanation())).queue();

    }

    public static void onJobQueued(Job job) { //This method is called when a job is queued, but before it starts running.
        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");

    }
}
