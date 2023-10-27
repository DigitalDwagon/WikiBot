package dev.digitaldragon.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JobManager {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static final Map<String, String> jobBuckets = new HashMap<>();
    private static final Map<String, Job> jobs = new HashMap<>();

    /**
     * Submits a job for execution.
     *
     * @param job the job to be submitted
     */
    public static void submit(Job job) {
        jobs.put(job.getId(), job);
        JobEvents.onJobQueued(job);
        executorService.submit(job::run);
    }

    /**
     * Aborts the execution of a job with the given id.
     *
     * @param id the id of the job to be aborted
     * @return {@code true} if the job was successfully aborted, {@code false} otherwise
     */
    public static boolean abort(String id) {
        Job job = jobs.get(id);
        if (job != null && job.isRunning()) {
            return job.abort();
        }

        return false;
    }

    /**
     * Retrieves the job with the given id.
     *
     * @param id the id of the job to retrieve
     * @return the job with the given id, or {@code null} if no such job exists
     */
    public static Job get(String id) {
        return jobs.get(id);
    }

    /**
     * Retrieves a list of active jobs.
     *
     * @return a list of active jobs, or an empty list if no active jobs exist
     */
    public static List<Job> getActiveJobs() {
        return jobs.values().stream().filter(Job::isRunning).collect(Collectors.toList());
    }

    /**
     * Retrieves a list of queued jobs.
     *
     * @return a list of queued jobs, or an empty list if no queued jobs exist
     */
    public static List<Job> getQueuedJobs() {
        return jobs.values().stream().filter(job -> job.getStatus() == JobStatus.QUEUED).collect(Collectors.toList());
    }


}
