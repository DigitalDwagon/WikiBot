package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.interfaces.irc.IRCClient;
import dev.digitaldragon.interfaces.irc.IRCFormat;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import net.badbird5907.lightning.annotation.EventHandler;

public class APIJobListener {
    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        UpdatesWebsocket.sendLogMessageToClients(event.getJob(), "SUCCESS");
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        UpdatesWebsocket.sendLogMessageToClients(event.getJob(), "FAILED");
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        UpdatesWebsocket.sendLogMessageToClients(event.getJob(), "ABORTED");

    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        UpdatesWebsocket.sendLogMessageToClients(event.getJob(), "QUEUED");
    }
}
