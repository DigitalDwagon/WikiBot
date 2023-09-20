package dev.digitaldragon.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JobManager {
    // between 1 and 99999 threads
    private static final ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static final Map<String, String> jobBuckets = new HashMap<>();
    private static final Map<String, Job> jobs = new HashMap<>();

    public static void submit(Job job) {
        jobs.put(job.getId(), job);
        JobEvents.onJobQueued(job);
        executorService.submit(job::run);
    }

    public static boolean abort(String id) {
        Job job = jobs.get(id);
        if (job != null && job.isRunning()) {
            return job.abort();
        }

        return false;
    }

    public static Job get(String id) {
        return jobs.get(id);
    }

    public static List<Job> getActiveJobs() {
        return jobs.values().stream().filter(Job::isRunning).collect(Collectors.toList());
    }

    public static List<Job> getQueuedJobs() {
        return jobs.values().stream().filter(job -> job.getStatus() == JobStatus.QUEUED).collect(Collectors.toList());
    }


}
