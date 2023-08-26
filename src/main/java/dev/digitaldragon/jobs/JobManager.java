package dev.digitaldragon.jobs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobManager {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
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


}
