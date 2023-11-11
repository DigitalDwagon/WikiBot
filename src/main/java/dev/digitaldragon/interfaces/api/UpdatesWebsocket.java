package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
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
public class UpdatesWebsocket {
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

    // Method to send a job event message to all connected clients.
    public static void sendLogMessageToClients(Job job, String event) {
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
}
