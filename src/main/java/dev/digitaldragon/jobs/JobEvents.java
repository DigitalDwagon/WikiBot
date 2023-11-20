package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.api.UpdatesWebsocket;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
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
        JobFailureEvent event = new JobFailureEvent(job);
        WikiBot.getBus().post(event);
    }

    /**
     * This method is called when a job succeeds.
     *
     * @param job The job that has succeeded.
     */
    public static void onJobSuccess(Job job) { //This method is called when a job succeeds.
        JobSuccessEvent event = new JobSuccessEvent(job);
        WikiBot.getBus().post(event);
    }

    /**
     * This method is called when a job fails because it was aborted while running.
     *
     * @param job The job that was aborted.
     */
    public static void onJobAbort(Job job) {
        JobAbortEvent event = new JobAbortEvent(job);
        WikiBot.getBus().post(event);
    }

    /**
     * This method is called when a job is queued, but before it starts running.
     *
     * @param job The job that was queued.
     */
    public static void onJobQueued(Job job) {
        JobQueuedEvent event = new JobQueuedEvent(job);
        WikiBot.getBus().post(event);
    }
}
