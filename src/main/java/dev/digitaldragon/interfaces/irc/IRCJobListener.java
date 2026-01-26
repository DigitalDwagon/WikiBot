package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobCompletedEvent;
import dev.digitaldragon.jobs.events.JobRunningEvent;
import net.badbird5907.lightning.annotation.EventHandler;

import java.time.Duration;
import java.time.Instant;

public class IRCJobListener {
    @EventHandler
    public void onJobCompleted(JobCompletedEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        if (meta.getPlatform() != null && meta.getPlatform() != JobMeta.JobPlatform.IRC) return;
        if (meta.getSilentMode() != JobMeta.SilentMode.ALL && meta.getSilentMode() != JobMeta.SilentMode.END) return;

        IRCClient.sendMessage(meta.getUserName(), String.format("%sSuccess!%s Job for %s %s(%s)%s completed.", IRCFormat.LIGHT_GREEN, IRCFormat.RESET, meta.getTargetUrl().orElse("unknown"), IRCFormat.GREY, job.getId(), IRCFormat.RESET));
        IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
        if (meta.getExplain().isPresent()) IRCClient.sendMessage("Explanation: " + meta.getExplain().get());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        if (meta.getPlatform() != null && meta.getPlatform() != JobMeta.JobPlatform.IRC) return;
        if (meta.getSilentMode() == JobMeta.SilentMode.SILENT) return;

        if (job.getFailedTaskCode() == 88 && job.getArchiveUrl() != null) {
            if (meta.getSilentMode() == JobMeta.SilentMode.DONE) return;
            IRCClient.sendMessage(meta.getUserName(), String.format("%s has %salready been archived%s in the past year! Use --force to override. %s(for %s)", meta.getTargetUrl().orElse("unknown"), IRCFormat.ORANGE, IRCFormat.RESET, IRCFormat.GREY, job.getId()));
            IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
            IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
            return;
        }

        IRCClient.sendMessage(String.format("%s%s: Job for %s%s %s(%s)%s failed with exit code %s.", IRCFormat.RED, meta.getUserName(), IRCFormat.RESET, meta.getTargetUrl().orElse("unknown"), IRCFormat.GREY, job.getId(), IRCFormat.RED, job.getFailedTaskCode()));
        if (meta.getExplain().isPresent()) IRCClient.sendMessage("Explanation: " + meta.getExplain().get());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        if (meta.getPlatform() != null && meta.getPlatform() != JobMeta.JobPlatform.IRC) return;
        if (meta.getSilentMode() == JobMeta.SilentMode.SILENT) return;

        IRCClient.sendMessage(meta.getUserName(), String.format("Your job for %s %s(%s)%s was %saborted.", meta.getTargetUrl().orElse("unknown"), IRCFormat.GREY, job.getId(), IRCFormat.RESET, IRCFormat.ORANGE));
        if (meta.getExplain().isPresent()) IRCClient.sendMessage("Explanation: " + meta.getExplain().get());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
    }

    @EventHandler
    public void onJobRunning(JobRunningEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        if (meta.getPlatform() != null && meta.getPlatform() != JobMeta.JobPlatform.IRC) return;
        if (meta.getSilentMode() != JobMeta.SilentMode.ALL) return;
        if (Duration.between(job.getStartTime(), Instant.now()).compareTo(Duration.ofSeconds(30)) < 0) return;

        IRCClient.sendMessage(meta.getUserName(), "Running job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");
    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        if (meta.getPlatform() != null && meta.getPlatform() != JobMeta.JobPlatform.IRC) return;
        if (meta.getSilentMode() != JobMeta.SilentMode.ALL) return;


        IRCClient.sendMessage(meta.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");
    }
}
