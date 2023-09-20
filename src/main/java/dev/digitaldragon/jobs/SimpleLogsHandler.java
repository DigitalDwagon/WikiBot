package dev.digitaldragon.jobs;

/**
 * StringLogHandler that prints messages to stdout
 */
public class SimpleLogsHandler implements StringLogHandler {
    public void onMessage(String message) {
        System.out.println(message);
    }
}
