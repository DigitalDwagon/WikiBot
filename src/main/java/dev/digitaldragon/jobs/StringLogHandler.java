package dev.digitaldragon.jobs;

/**
 * This interface represents a handler for processing log messages as strings.
 */
public interface StringLogHandler {
    void onMessage(String message);
}
