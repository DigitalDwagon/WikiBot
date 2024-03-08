package dev.digitaldragon.jobs;

import net.dv8tion.jda.api.entities.ThreadChannel;

import java.io.File;
import java.time.Instant;
import java.util.List;

/**
 * The Job interface represents a job that can be executed.
 * It provides methods to control and retrieve information about the job's execution.
 * <p>
 * This interface also provides methods for managing the job's properties and metadata.
 */
public interface Job {
    /**
     * Starts the job. Run in a separate thread. You are *highly encouraged* to use the
     * {@link JobManager} class to run jobs.
     */
    public void run();
    /**
     * Aborts the job.
     *
     * @return true if the job was successfully aborted, false otherwise.
     */
    public boolean abort();
    /**
     * Returns whether the job is currently running.
     *
     * @return true if the job is running, false otherwise.
     */
    public boolean isRunning();
    /**
     * Returns the ID of the job.
     *
     * @return the ID of the job as a string.
     */
    public String getId();
    /**
     * Returns the name of the job. This is a user-friendly name, usually the target URL.
     *
     * @return the name of the job as a string.
     */
    public String getName();
    /**
     * Returns the username associated with the job.
     *
     * @return the username associated with the job as a string.
     */
    public String getUserName();
    /**
     * Gets the user's platform (IRC, Telegram, Discord, etc.)
     *
     */
    //public String getUserPlatform();
    /**
     * Gets the user's ID from their platform (ie Discord User ID, IRC NickServ account name, etc.)
     *
     */
    //public String getUserPlatformId();
    /**
     * Returns the user's explanation for starting a job.
     *
     * @return the explanation of the job as a string.
     */
    public String getExplanation();
    /**
     * Returns the status of the job.
     *
     * @return the status of the job as a JobStatus object.
     */
    public JobStatus getStatus();
    /**
     * Returns the type of the job.
     *
     * @return the type of the job as a JobType object.
     */
    public JobType getType();
    /**
     * Returns the running task of the job.
     *
     * @return the running task of the job as a String.
     */
    public String getRunningTask();
    /**
     * Returns a list of all tasks of the job.
     *
     * @return a list of all tasks of the job as a List of Strings.
     */
    public List<String> getAllTasks();
    /**
     * Returns the start time of the job.
     *
     * @return the start time of the job as an Instant object.
     */
    public Instant getStartTime();
    /**
     * Returns the URL of the logs for the task.
     *
     * @return the URL of the logs for the task as a String. If the logs have not been uploaded yet,
     * or the job is not complete, returns null.
     */
    public String getLogsUrl();
    /**
     * Returns the URL of the archive.org item for the finished job.
     *
     * @return the archive.org URL for the task as a String. If the archive has not been uploaded yet,
     * or the job is not complete, returns null.
     */
    public String getArchiveUrl();
    /**
     * Returns the directory where the job files are stored.
     *
     * @return the directory where the job files are stored as a File object.
     */
    public File getDirectory();
    /**
     * Returns the Discord thread channel associated with the current thread.
     *
     * @return the thread channel associated with the current thread.
     */
    public void setArchiveUrl(String url);
    /**
     * Gets the code of the failed task, if any.
     *
     * @return the error code of the failed task, or 0 if no task has failed.
     */
    public int getFailedTaskCode();
}
