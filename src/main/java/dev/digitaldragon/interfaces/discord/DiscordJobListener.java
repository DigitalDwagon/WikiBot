package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.irc.IRCClient;
import dev.digitaldragon.interfaces.irc.IRCFormat;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import dev.digitaldragon.util.EnvConfig;
import net.badbird5907.lightning.annotation.EventHandler;
import net.dv8tion.jda.api.entities.TextChannel;

public class DiscordJobListener {
    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        Job job = event.getJob();

        TextChannel successChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_success_channel"));
        if (successChannel != null)
            successChannel.sendMessage(String.format("%s for %s:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nArchive URL: %s\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getArchiveUrl(), job.getExplanation())).queue();

        job.getThreadChannel().sendMessage("Job ended.").queue();
        job.getThreadChannel().sendMessage("Explanation: ```" + job.getExplanation() + "```").queue();
        job.getThreadChannel().sendMessage("Logs are available at " + job.getLogsUrl()).queue();
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        Job job = event.getJob();

        TextChannel failChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_failure_channel"));
        if (failChannel != null)
            failChannel.sendMessage(String.format("%s for %s:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nFailed Task: %s\nExit code: `%s`\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getRunningTask(), job.getFailedTaskCode(), job.getExplanation())).queue();

        job.getThreadChannel().sendMessage("Job " + job.getId() + " failed with exit code " + job.getFailedTaskCode() + ".").queue();
        job.getThreadChannel().sendMessage("Explanation: ```" + job.getExplanation() + "```").queue();
        job.getThreadChannel().sendMessage("Logs are available at " + job.getLogsUrl()).queue();
        job.getThreadChannel().sendMessage("Task indicated as failed.").queue();
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        Job job = event.getJob();

        TextChannel failChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_failure_channel"));
        if (failChannel != null)
            failChannel.sendMessage(String.format("%s for %s was aborted:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nFailed Task: %s\nExit code: `%s`\nExplanation: ```%s```", job.getName(), job.getUserName(), job.getThreadChannel(), job.getLogsUrl(), job.getId(), job.getRunningTask(), job.getFailedTaskCode(), job.getExplanation())).queue();
    }
}
