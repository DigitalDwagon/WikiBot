package dev.digitaldragon.jobs;

public class GenericLogsHandler {
    private static Job job;
    public GenericLogsHandler(Job job) {
        this.job = job;
    }

    public void onMessage(String message) {
        System.out.println(message);


        if (message.contains("https://archive.org/details/") && message.contains(" ")) {
            String[] split = message.split(" ");
            for (String s : split) {
                if (s.contains("https://archive.org/details/")) {
                    job.setArchiveUrl(s);
                    break;
                }
            }
        } else if (message.contains("https://archive.org/details/")) {
            job.setArchiveUrl(message);
        }
    }
}
