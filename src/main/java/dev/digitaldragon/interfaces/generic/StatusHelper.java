package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;

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
            return JobManager.getRunningJobs().size() + " running jobs. " + JobManager.getQueuedJobs().size() + " jobs waiting to run.";
        Job job = JobManager.get(jobId);
        if (job == null)
            return "Job " + jobId + " does not exist!";
        JobMeta meta = job.getMeta();

        return String.format("Job for %s (%s) is %s. In queue \"%s\". %s%s",
                meta.getTargetUrl().orElse("unknown"),
                job.getId(),
                job.getStatus().name().toLowerCase(),
                job.getMeta().getQueue(),
                job.getMeta().getExplain().isPresent() ? "Explanation: \"" + job.getMeta().getExplain().get() + "\" " : "",
                job.getLogsUrl() != null ? job.getLogsUrl() : ""
                );
    }
}
