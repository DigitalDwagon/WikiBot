package dev.digitaldragon.jobs;

/**
 * Represents the status of a job.
 */
public enum JobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    ABORTED
}
