package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.ReuploadJob;

import java.util.UUID;

public class ReuploadHelper {
    /**
     * Starts a reupload job. Returns a user-friendly message about the status of the job.
     *
     * @param jobId the ID of the job to begin
     * @param userName the name of the user initiating the job
     * @return null
     */
    public static String beginJob(String jobId, String userName) {
        Job job = new ReuploadJob(userName, UUID.randomUUID().toString(), jobId);
        JobManager.submit(job);

        return null;
    }
}
