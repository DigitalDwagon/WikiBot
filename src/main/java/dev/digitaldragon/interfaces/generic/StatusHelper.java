package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;

import java.time.Duration;
import java.time.Instant;

public class StatusHelper {
    /**
     * Returns a user-friendly message about the status of the job with the given ID.
     * The ID may be null, in which case the general system status is returned.
     *
     * @param jobId the ID of the job
     * @return the status of the job
     */
    public static String getStatus(String jobId) {
        if (jobId == null)
            return JobManager.getActiveJobs().size() + " running jobs. " + JobManager.getQueuedJobs().size() + " jobs waiting to run.";
        Job job = JobManager.get(jobId);
        if (job == null)
            return "Job " + jobId + " does not exist!";
        JobMeta meta = job.getMeta();


        StringBuilder message = new StringBuilder();
        message.append("Job ").append(jobId);
        if (meta.getTargetUrl().isPresent()) {
            message.append(String.format(" | %s (%s)", meta.getTargetUrl().get(), job.getId()));
        } else {
            message.append(" | ").append(job.getId());
        }
        message.append(" is ");

        if (job.isRunning()) {
            message.append("running");
        } else {
            message.append("not running");
        }

        if (job.getRunningTask() != null) {
            message.append(" (task ").append(job.getRunningTask()).append("). ");
        } else {
            message.append(". ");
        }

        message.append("Status: ");
        message.append(job.getStatus().toString());
        message.append(". ");
        if (job.getStartTime() != null) {
            message.append("Started: ");
            message.append(Duration.between(job.getStartTime(), Instant.now()).toSeconds()).append(" seconds ago. ");
        }

        if (meta.getExplain().isPresent()) message.append("\"").append(meta.getExplain().get()).append("\"");

        return message.toString();
    }
}
