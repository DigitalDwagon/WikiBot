package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.jobs.events.JobLogEvent;
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
}
