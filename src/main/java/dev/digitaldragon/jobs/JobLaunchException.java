package dev.digitaldragon.jobs;

public class JobLaunchException extends RuntimeException {
    public JobLaunchException(String message) {
        super(message);
    }

    public JobLaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
