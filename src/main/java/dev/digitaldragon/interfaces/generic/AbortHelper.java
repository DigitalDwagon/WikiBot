package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.jobs.JobManager;

public class AbortHelper {
    public static String abortJob(String jobId) {
        if (JobManager.abort(jobId))
            return "Aborted job " + jobId + "!";
        else
            return "Failed to abort job " + jobId + "! It might not exist, be in a task that can't be aborted, or have already finished.";
    }
}
