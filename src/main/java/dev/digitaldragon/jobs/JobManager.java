package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import lombok.Getter;
import org.json.JSONObject;

import java.util.*;
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

    public static Integer getQueueConcurrency(String queue) {
        queue = queue.toLowerCase();
        return queueConcurrency.get(queue) == null ? 0 : queueConcurrency.get(queue);
    }

    public static void setQueueConcurrency(String queue, int concurrency) {
        queue = queue.toLowerCase();
        queueConcurrency.put(queue, concurrency);
        if (queuePriority.get(queue) == null) setQueuePriority(queue, 0);
    }

    public static Integer getQueuePriority(String queue) {
        queue = queue.toLowerCase();
        return queuePriority.get(queue) == null ? 0 : queuePriority.get(queue);
    }

    public static void setQueuePriority(String queue, int priority) {
        queue = queue.toLowerCase();
        queuePriority.put(queue, priority);
        if (queueConcurrency.get(queue) == null) setQueueConcurrency(queue, 0);

    }

    public static void launchJobs() {
        if (pendingJobs.isEmpty()) return;
        if (runningJobs.size() >= MAX_CONCURRENCY) return;
        Map<String, Integer> runningJobsPerQueue = new HashMap<>();
        runningJobs.forEach(jobId -> {
            String queue = get(jobId).getMeta().getQueue();
            runningJobsPerQueue.computeIfAbsent(queue, (key) -> 0);
            runningJobsPerQueue.put(queue, runningJobsPerQueue.get(queue) + 1);
        });

        Map<String, List<String>> pendingJobsPerQueue = new HashMap<>();
        pendingJobs.forEach(jobId -> {
            String queue = get(jobId).getMeta().getQueue();
            pendingJobsPerQueue.computeIfAbsent(queue, (key) -> new ArrayList<>());
            pendingJobsPerQueue.get(queue).add(jobId);
        });
        if (pendingJobsPerQueue.isEmpty()) return;

        pendingJobsPerQueue.keySet().stream()
                .sorted(Comparator.comparingInt((key) -> {
                    return -getQueuePriority(key); // Sorts smallest to greatest by default, so flip the prio numbers around
                })) // Sort by priority
                .forEach(queue -> {
                    pendingJobsPerQueue.get(queue).forEach(jobId -> {
                        if (runningJobs.size() >= MAX_CONCURRENCY) return;
                        runningJobsPerQueue.computeIfAbsent(queue, (key) -> 0);
                        if (runningJobsPerQueue.get(queue) >= getQueueConcurrency(queue)) return;
                        pendingJobs.remove(jobId);
                        runningJobs.add(jobId);
                        executorService.submit(() -> {
                            try {
                                get(jobId).run();
                            } catch (Exception e) {
                                e.printStackTrace();
                                //TODO - mark the job failed if this happens
                            }
                            runningJobs.remove(jobId);
                            launchJobs();
                        });
                    });
                });
    }

    public static JSONObject getJsonForJob(Job job) {
        JobMeta meta = job.getMeta();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", job.getStatus());
        if (meta.getExplain().isPresent()) jsonObject.put("explanation", meta.getExplain().get());
        jsonObject.put("user", meta.getUserName());
        jsonObject.put("started", job.getStartTime());
        if (meta.getExplain().isPresent()) jsonObject.put("name", meta.getExplain().get());
        jsonObject.put("runningTask", job.getRunningTask());
        //jsonObject.put("directory", job.getDirectory());
        jsonObject.put("failedTaskCode", job.getFailedTaskCode());
        jsonObject.put("archiveUrl", job.getArchiveUrl());
        jsonObject.put("type", job.getType());
        jsonObject.put("isRunning", job.isRunning());
        jsonObject.put("allTasks", job.getAllTasks());
        jsonObject.put("logsUrl", job.getLogsUrl());
        return jsonObject;
    }

    public static JSONObject getJsonForJob(String jobId) {
        Job job = jobs.get(jobId);
        if (job != null) {
            return getJsonForJob(job);
        } else {
            return null;
        }
    }

    public static void submitJobDbOnly(Job job) {
        jobs.put(job.getId(), job);
    }

    public static void clearDb(String jobId) {
        jobs.remove(jobId);
    }

}
