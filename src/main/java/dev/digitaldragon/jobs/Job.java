package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.events.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The Job interface represents a job that can be executed.
 * It provides methods to control and retrieve information about the job's execution.
 * <p>
 * This interface also provides methods for managing the job's properties and metadata.
 */
public abstract class Job {
    private static final Logger logger = LoggerFactory.getLogger(Job.class);
    /**
     * The time that the job was started, or null if it has not yet started.
     */
    @Getter
    @Nullable
    private Instant startTime = null;
    @Getter
    @Setter
    private JobStatus status = JobStatus.QUEUED;
    /**
     * The URL of the logs for the job. Returns null if the job has not yet completed or there was an error uploading logs.
     */
    @Getter
    private String logsUrl = null;
    /**
     * The exit code of the task that failed, if any. 0 if no tasks have failed.
     */
    @Getter
    private int failedTaskCode = 0;
    /**
     * URL of the uploaded archive.org item after the job completes, or null if there is no item.
     */
    @Getter
    @Setter
    @Nullable
    private String archiveUrl = null;



    /**
     * The raw job code.
     * @return A JobResult indicating how execution of the job went.
     */
    protected abstract JobResult execute();
    /**
     * Aborts the job.
     *
     * @return true if the job was successfully aborted, false otherwise.
     */
    public abstract boolean abort();
    /**
     * Returns the ID of the job.
     *
     * @return the ID of the job as a string.
     */
    public abstract String getId();
    /**
     * Returns the type of the job.
     *
     * @return the type of the job as a JobType object.
     */
    public abstract JobType getType();
    /**
     * Returns the running task of the job.
     *
     * @return the running task of the job as a String.
     */
    public abstract String getRunningTask();
    /**
     * Returns a list of all tasks of the job.
     *
     * @return a list of all tasks of the job as a List of Strings.
     */
    public abstract List<String> getAllTasks();
    /**
     * Returns the directory where the job files are stored.
     *
     * @return the directory where the job files are stored as a File object.
     */
    public abstract File getDirectory();

    /**
     * Gets the job's metadata.
     * @return the job's metadata as a JobMeta object.
     */
    public abstract JobMeta getMeta();

    /**
     * Starts the job. Run in a separate thread. You are *highly encouraged* to use the
     * {@link JobManager} class to run jobs.
     */
    public void run() {
        if (status == JobStatus.ABORTED) return;
        this.startTime = Instant.now();
        this.status = JobStatus.RUNNING;
        WikiBot.getBus().post(new JobRunningEvent(this));

        log("wikibot v" + WikiBot.getVersion() + " job " + getId());

        JobResult result;
        try {
            result = this.execute();
        } catch (Exception e) {
            logger.error("An exception occurred in-process for job {}:", getId(), e);
            log("An exception occurred while executing job code: ");
            log(e);

            result = new JobResult(false, 999);
        }
        // No logging can happen after this point.
        logsUrl = CommonTasks.uploadLogs(this);

        if (result.isSuccess()) {
            status = JobStatus.COMPLETED;
            WikiBot.getBus().post(new JobCompletedEvent(this));
            return;
        }

        failedTaskCode = result.getExitCode();

        if (status == JobStatus.ABORTED) {
            WikiBot.getBus().post(new JobAbortEvent(this));
            return;
        }

        status = JobStatus.FAILED;
        WikiBot.getBus().post(new JobFailureEvent(this));
    }


    /**
     * Returns whether the job is currently running.
     *
     * @return true if the job is running, false otherwise.
     */
    public boolean isRunning() {
        return this.status == JobStatus.RUNNING;
    }

    /**
     * Adds a log message to the job's log.
     * @param message the message to add to the log
     */
    public void log(String message) {
        WikiBot.getBus().post(new JobLogEvent(this, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(Instant.now().atZone(ZoneOffset.UTC)) + " | " + message));
    }

    /**
     * Logs the stack trace of a throwable to the job's log.
     * @param throwable The throwable with a stack trace to log.
     */
    public void log(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);

        for (String line : sw.toString().split("\\R")) {
            this.log(line);
        }
    }
}
