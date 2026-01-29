package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.events.JobLogEvent;
import dev.digitaldragon.jobs.events.JobCompletedEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobRunningEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import io.javalin.websocket.WsConfig;
import net.badbird5907.lightning.annotation.EventHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LogWebsocket implements Consumer<WsConfig> {
    private final Map<Session, String> connectedClients = new ConcurrentHashMap<>();

    // Method to send a log message for job events to all connected clients.
    public void sendEventLogMessageToClients(Job job, String event) {
        WikiBot.getBus().post(new JobLogEvent(job, "")); // Extra Line break
        job.log(String.format("Job %s is now %s", job.getId(), event.toLowerCase()));
    }

    @Override
    public void accept(WsConfig wsConfig) {
        wsConfig.onConnect(ctx -> {
            ctx.enableAutomaticPings();
            connectedClients.put(ctx.session, ctx.session.getRemoteAddress().toString());
        });
        wsConfig.onClose(ctx -> connectedClients.remove(ctx.session));
        wsConfig.onMessage(ctx -> {});
    }

    @EventHandler
    public void onJobLog(JobLogEvent event) {
        JSONObject json = new JSONObject();
        json.put("jobId", event.getJob().getId());
        json.put("logLine", event.getMessage());

        for (Session session : connectedClients.keySet()) {
            try {
                session.getRemote().sendString(json.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onJobCompleted(JobCompletedEvent event) {
        sendEventLogMessageToClients(event.getJob(), "COMPLETED");
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        sendEventLogMessageToClients(event.getJob(), "FAILED");
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        sendEventLogMessageToClients(event.getJob(), "ABORTED");
    }

    @EventHandler
    public void onJobRunning(JobRunningEvent event) {
        sendEventLogMessageToClients(event.getJob(), "RUNNING");
    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        sendEventLogMessageToClients(event.getJob(), "QUEUED");
    }


}
