package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
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

        if (job.getFailedTaskCode() == 88 && job.getArchiveUrl() != null) {
            IRCClient.sendMessage(job.getUserName(), String.format("%s has %salready been archived%s in the past year! Use --force to override. %s(for %s)", job.getName(), IRCFormat.ORANGE, IRCFormat.RESET, IRCFormat.GREY, job.getId()));
            IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
            IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
            return;
        }

        IRCClient.sendMessage(String.format("%s%s: Job for %s%s %s(%s)%s failed with exit code %s.", IRCFormat.RED, job.getUserName(), IRCFormat.RESET, job.getName(), IRCFormat.GREY, job.getId(), IRCFormat.RED, job.getFailedTaskCode()));
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        Job job = event.getJob();
        IRCClient.sendMessage(job.getUserName(), String.format("Your job for %s %s(%s)%s was %saborted.", job.getName(), IRCFormat.GREY, job.getId(), IRCFormat.RESET, IRCFormat.ORANGE));
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        Job job = event.getJob();
        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");
    }
}
