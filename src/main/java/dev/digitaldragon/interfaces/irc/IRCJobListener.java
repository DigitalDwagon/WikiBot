package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import net.badbird5907.lightning.annotation.EventHandler;

public class IRCJobListener {
    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        Job job = event.getJob();
        IRCClient.sendMessage(job.getUserName(), "Success! Job " + job.getId() + " completed successfully.");
        IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());
    }
}
