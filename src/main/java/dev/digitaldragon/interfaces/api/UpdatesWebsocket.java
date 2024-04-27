package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import io.javalin.websocket.WsConfig;
import net.badbird5907.lightning.annotation.EventHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@WebSocket
public class UpdatesWebsocket implements Consumer<WsConfig> {
    private final Map<Session, String> connectedClients = new ConcurrentHashMap<>();

    // Method to send a job event message to all connected clients.
    public void sendLogMessageToClients(Job job, String event) {
        JSONObject json = new JSONObject();
        json.put("jobId", job.getId());
        json.put("event", event);
        json.put("info", JobManager.getJsonForJob(job));

        for (Session session : connectedClients.keySet()) {
            try {
                session.getRemote().sendString(json.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void accept(WsConfig wsConfig) {
        wsConfig.onConnect(ctx -> connectedClients.put(ctx.session, ctx.session.getRemoteAddress().toString()));
        wsConfig.onClose(ctx -> connectedClients.remove(ctx.session));
        wsConfig.onMessage(ctx -> {});
    }

    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        sendLogMessageToClients(event.getJob(), "SUCCESS");
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        sendLogMessageToClients(event.getJob(), "FAILED");
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        sendLogMessageToClients(event.getJob(), "ABORTED");

    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        sendLogMessageToClients(event.getJob(), "QUEUED");
    }


}
