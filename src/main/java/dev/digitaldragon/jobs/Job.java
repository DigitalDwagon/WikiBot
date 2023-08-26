package dev.digitaldragon.jobs;

import java.time.Instant;
import java.util.List;

public interface Job {
    public void run();
    public boolean abort();
    public boolean isRunning();
    public String getId();
    public String getName();
    public String getUserName();
    public JobStatus getStatus();
    public JobType getType();
    public String getRunningTask();
    public List<String> getAllTasks();
    public Instant getStartTime();
}
