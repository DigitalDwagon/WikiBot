package dev.digitaldragon.db;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.JobStatus;
import dev.digitaldragon.jobs.JobType;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.time.Instant;
import java.util.List;

/**
 * The LoadedJob class represents a job that has been loaded from the database.
 * This class extends the Job class.
 * The LoadedJob class provides methods for setting the job's metadata but can't actually be run.
 */
@Getter
@Setter
public class LoadedJob extends Job {
    private String id = null;
    private String name = "undefined";
    private String userName = "undefined";
    private String explanation = null;
    private JobStatus status = null;
    private JobType type = null;
    private String runningTask = null;
    private Instant startTime = null;
    private String logsUrl = null;
    private String archiveUrl = null;
    public File directory = null;
    private int failedTaskCode = 0;
    private JobMeta meta = new JobMeta("undefined");

    public void run() {
        throw new UnsupportedOperationException("Loaded jobs cannot be run.");
    }

    public boolean abort() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    @Override
    public List<String> getAllTasks() {
        return List.of();
    }


}
