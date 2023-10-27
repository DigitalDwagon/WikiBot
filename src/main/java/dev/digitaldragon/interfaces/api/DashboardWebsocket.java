package dev.digitaldragon.interfaces.api;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@WebSocket
public class DashboardWebsocket {
    private static final Map<Session, String> connectedClients = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session userSession) throws Exception {
        // Handle WebSocket connection.
        connectedClients.put(userSession, userSession.getRemoteAddress().toString());
    }

    @OnWebSocketClose
    public void onClose(Session userSession, int statusCode, String reason) {
        // Handle WebSocket closure.
        connectedClients.remove(userSession);
    }

    @OnWebSocketMessage
    public void onMessage(Session userSession, String message) {
        // Handle incoming WebSocket messages (if needed).
    }

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
}
