package dev.digitaldragon.interfaces.api;

import io.javalin.websocket.WsConfig;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LogWebsocket implements Consumer<WsConfig> {
    private static final Map<Session, String> connectedClients = new ConcurrentHashMap<>();

    // Method to send a log message to all connected clients.
    public static void sendLogMessageToClients(String jobId, String logLine) {
        JSONObject json = new JSONObject();
        json.put("jobId", jobId);
        json.put("logLine", logLine);

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
}
