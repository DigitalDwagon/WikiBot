package dev.digitaldragon.jobs;

import dev.digitaldragon.jobs.events.JobLogEvent;
import dev.digitaldragon.util.UploadObject;
import net.badbird5907.lightning.annotation.EventHandler;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class LogFiles {
    private final Map<String, File> logFiles = new HashMap<>();

    public void setLogFile(Job job, File file) {
        logFiles.put(job.getId(), file);
    }

    public File getLogFile(Job job) {
        return logFiles.get(job.getId());
    }

    @EventHandler
    public void onJobLog(JobLogEvent event) {
        File logFile = logFiles.get(event.getJob().getId());
        if (logFile == null) {
            logFile = new File(event.getJob().getDirectory(), "log.txt");
            logFiles.put(event.getJob().getId(), logFile);
        }

        try {
            Files.writeString(logFile.toPath(), event.getMessage() + "\n", Files.exists(logFile.toPath()) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
        } catch (Exception e) {
            LoggerFactory.getLogger(LogFiles.class).error("Error writing to log file", e);
        }
    }

    public String uploadLogs(Job job) {
        String jobId = job.getId();
        try {
            UploadObject.uploadObject("digitaldragons", "cdn.digitaldragon.dev", "wikibot/jobs/" + jobId + "/log.txt", logFiles.get(job.getId()).getAbsolutePath(), "text/plain; charset=utf-8", "inline");
        } catch (IOException e) {
            e.printStackTrace();
            return "Error uploading logs. Sorry :(";
        }
        return String.format("https://cdn.digitaldragon.dev/wikibot/jobs/%s/log.txt", jobId);
    }
}
