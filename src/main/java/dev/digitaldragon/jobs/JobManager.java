package dev.digitaldragon.jobs;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JobManager {
    // between 1 and 99999 threads
    private static final ExecutorService executorService = Executors.newFixedThreadPool(100);
    private static final Map<String, String> jobBuckets = new HashMap<>();
    private static final Map<String, Job> jobs = new HashMap<>();
    private static final PriorityQueue<Job> jobQueue = new PriorityQueue<>();
    private static final int MAX_CONCURRENT = 2;
    private static final int UPLOADS_LIMIT = 30;

    public static void submit(Job job) {
        jobs.put(job.getId(), job);
        JobEvents.onJobQueued(job);
        if (canAddJob())
            executorService.submit(job::run);
        else
            jobQueue.add(job);
    }

    public static void submit(Job job, String bucket) {

    }

    public static void pokeQueue() {
        if (canAddJob() && !jobQueue.isEmpty()) {
            Job job = jobQueue.poll();
            executorService.submit(job::run);
        }
    }

    private static boolean canAddJob() {
        int i = 0;
        int hardlimit = 0;
        for (Job job : jobs.values()) {
            if (job.isRunning()) {
                if (job.getRunningTask().equals("UploadMediaWiki")) {
                    hardlimit++;
                } else {
                    i++;
                }
            }

        }
        return i < MAX_CONCURRENT && hardlimit < UPLOADS_LIMIT;
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
