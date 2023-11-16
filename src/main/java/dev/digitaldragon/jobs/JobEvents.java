package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.api.UpdatesWebsocket;
import dev.digitaldragon.util.EnvConfig;
import dev.digitaldragon.interfaces.irc.IRCClient;
import net.dv8tion.jda.api.entities.TextChannel;

public class JobEvents {
    /**
     * This method is called when a job fails (due to an improper task exit code, etc, as dictated by the job).
     * The runningTask is the task that failed.
     *
     * @param job The job that has failed.
     */
    public static void onJobFailure(Job job) { //This method is called when a job fails (due to an improper task exit code, etc, as dictated by the job). The runningTask is the task that failed
        UpdatesWebsocket.sendLogMessageToClients(job, "FAILED");


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

    /**
     * This method is called when a job succeeds.
     *
     * @param job The job that has succeeded.
     */
    public static void onJobSuccess(Job job) { //This method is called when a job succeeds.
        UpdatesWebsocket.sendLogMessageToClients(job, "SUCCESS");

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

    /**
     * This method is called when a job fails because it was aborted while running.
     *
     * @param job The job that was aborted.
     */
    public static void onJobAbort(Job job) { //This method is called when a job fails because it was aborted while running.
        UpdatesWebsocket.sendLogMessageToClients(job, "ABORTED");
        IRCClient.sendMessage(job.getUserName(), "Your job " + job.getId() + " was aborted.");
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());

        TextChannel failChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_failure_channel"));
        if (failChannel != null)
            failChannel.sendMessage(String.format("%s for %s was aborted:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nFailed Task: %s\nExit code: `%s`\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getRunningTask(), job.getFailedTaskCode(), job.getExplanation())).queue();
    }

    /**
     * This method is called when a job is queued, but before it starts running.
     *
     * @param job The job that was queued.
     */
    public static void onJobQueued(Job job) { //This method is called when a job is queued, but before it starts running.
        UpdatesWebsocket.sendLogMessageToClients(job, "QUEUED");
        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");
    }
}
