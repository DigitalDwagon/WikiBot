package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import net.badbird5907.lightning.annotation.EventHandler;

public class IRCJobListener {
    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        Job job = event.getJob();
        IRCClient.sendMessage(job.getUserName(), String.format("%sSuccess!%s Job for %s %s(%s)%s completed.", IRCFormat.LIGHT_GREEN, IRCFormat.RESET, job.getName(), IRCFormat.GREY, job.getId(), IRCFormat.RESET));
        IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        Job job = event.getJob();
        IRCClient.sendMessage(String.format("%s%s: Job for %s%s %s(%s)%s failed with exit code %s.", IRCFormat.RED, job.getUserName(), IRCFormat.RESET, job.getName(), IRCFormat.GREY, job.getId(), IRCFormat.RED, job.getFailedTaskCode()));
        IRCClient.sendMessage(IRCFormat.RED + "Explanation: " + IRCFormat.RESET + job.getExplanation());
        IRCClient.sendMessage(IRCFormat.RED + "Logs URL: " + IRCFormat.RESET + job.getLogsUrl());
    }
}
