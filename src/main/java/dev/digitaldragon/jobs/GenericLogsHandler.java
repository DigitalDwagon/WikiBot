package dev.digitaldragon.jobs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A StringLogHandler that writes logs to log.txt and a Discord channel.
 */
@Deprecated
public class GenericLogsHandler implements StringLogHandler {
    private final Job job;

    public GenericLogsHandler(Job job) {
        this.job = job;
    }

    @Deprecated
    public synchronized void onMessage(String message) {
        job.log(message);
    }

    private static void writeLineToFile(File file, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.append(line);
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public void end() {

    }
}
