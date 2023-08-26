package dev.digitaldragon.jobs;

import dev.digitaldragon.util.IRCClient;

public class JobEvents {
    public static void onJobFailure(Job job) {
        //log to discord
    }

    public static void onJobSuccess(Job job) {
        IRCClient.sendMessage(job.getUserName(), "Success! Job " + job.getId() + " completed successfully.");
        IRCClient.sendMessage("Archive URL: " + job.getArchiveUrl());
        IRCClient.sendMessage("Explanation: " + job.getExplanation());
        IRCClient.sendMessage("Logs URL: " + job.getLogsUrl());

    }

    public static void onJobAbort(Job job) {
        IRCClient.sendMessage(job.getUserName(), "Your job " + job.getId() + " was aborted on task " + job.getRunningTask() + ".");
    }

    public static void onJobQueued(Job job) {
        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");

    }

    public static void onJobLogLine(Job job, String log) {
        //log to discord
    }
}
