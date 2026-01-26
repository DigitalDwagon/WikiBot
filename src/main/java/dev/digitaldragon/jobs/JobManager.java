package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.queues.Queue;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JobManager {
    private static final int MAX_CONCURRENCY = 15;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENCY);
    private static final Map<String, Job> jobs = new HashMap<>();
    private static final List<String> pendingJobs = new ArrayList<>();
    private static final List<String> runningJobs = new ArrayList<>();
    @Getter
    private static final Map<String, Integer> queueConcurrency = new HashMap<>();
    @Getter
    private static final Map<String, Integer> queuePriority = new HashMap<>();

    /**
     * Submits a job for execution.
     *
     * @param job the job to be submitted
     */
    public static void submit(Job job) {
        jobs.put(job.getId(), job);
        WikiBot.getBus().post(new JobQueuedEvent(job));
        pendingJobs.add(job.getId());
        launchJobs();
    }

    /**
     * Aborts the execution of a job with the given id.
     *
     * @param id the id of the job to be aborted
     * @return {@code true} if the job was successfully aborted, {@code false} otherwise
     */
    public static boolean abort(String id) {
        Job job = jobs.get(id);
        if (job != null && (job.isRunning() || job.getStatus() == JobStatus.QUEUED)) {
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
     * Retrieves a list of running jobs.
     *
     * @return a list of running jobs, or an empty list if no running jobs exist
     */
    public static List<Job> getRunningJobs() {
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

    public static List<Job> getJobs() {
        return new ArrayList<>(jobs.values());
    }

    public static void launchJobs() {
        if (pendingJobs.isEmpty() || runningJobs.size() >= MAX_CONCURRENCY) return;

        Map<Integer, List<String>> jobsByPriority = new HashMap<>();
        for (String jobId : pendingJobs) {
            Integer priority = WikiBot.getQueueManager().getQueue(get(jobId).getMeta().getQueue()).getPriority();
            jobsByPriority.computeIfAbsent(priority, k -> new ArrayList<>()).add(jobId);
        }

        jobsByPriority.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> e.getValue().forEach(jobId -> {
                    Queue queue = WikiBot.getQueueManager().getQueue(get(jobId).getMeta().getQueue());
                    if (getRunningJobsInQueue(queue.getName()) >= queue.getConcurrency()) return;
                    if (runningJobs.size() >= MAX_CONCURRENCY) return;

                    startJob(jobId);
                }));
    }

    private static void startJob(String jobId) {
        pendingJobs.remove(jobId);
        runningJobs.add(jobId);

        Job job = get(jobId);
        executorService.submit(() -> {
            try {
                job.run();
            } catch (Exception exception) {
                exception.printStackTrace();
                job.setStatus(JobStatus.FAILED);
                WikiBot.getBus().post(new JobFailureEvent(job));
            } finally {
                runningJobs.remove(jobId);
                if (job.getStatus() == JobStatus.RUNNING) {
                    job.setStatus(JobStatus.FAILED);
                }
                launchJobs();
            }
        });
    }

    public static int getRunningJobsInQueue(String queue) {
        queue = queue.toLowerCase();
        int running = 0;
        for (String jobId : runningJobs) {
            if (get(jobId).getMeta().getQueue().equals(queue)) running++;
        }
        return running;
    }

    public static void submitJobDbOnly(Job job) {
        jobs.put(job.getId(), job);
    }

    public static void clearDb(String jobId) {
        jobs.remove(jobId);
    }

}
