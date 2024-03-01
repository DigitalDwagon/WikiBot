package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import lombok.SneakyThrows;

import java.io.InputStream;

import static dev.digitaldragon.interfaces.api.JavalinAPI.app;

public class Dashboard {
    public static void register() {
        index();
        cards();
    }


    @SneakyThrows
    private static void index() {
        InputStream stream = Dashboard.class.getClassLoader().getResourceAsStream("dashboard.html");
        String content = new String(stream.readAllBytes());
        app.get("/", (ctx) -> {
            ctx.res().setContentType("text/html");
            StringBuilder cards =new StringBuilder();
            for (Job job : JobManager.getActiveJobs()) {
                cards.append(getCard(job));
            }
            ctx.result(content.replace("{active_jobs}", String.valueOf(JobManager.getActiveJobs().size()))
                    .replace("{queued_jobs}", String.valueOf(JobManager.getQueuedJobs().size()))
                    .replace("{job_cards}", cards));
        });

    }

    private static void cards() {
        app.get("/cards/{id}", (ctx) -> {
            ctx.res().setContentType("text/html");
            String id = ctx.pathParam("id");
            Job job = JobManager.get(id);
            if (job == null) {
                ctx.result("Job not found!");
            }
            ctx.result(getCard(job));
        });
    }

    private static String getCard(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"job-container\">");
        if (job.getName().startsWith("http://") || job.getName().startsWith("https://")) {
            sb.append(String.format("<h3 class=\"job-url\"><a href=\"%s\">%s</a></h3>", job.getName(), job.getName()));
        } else {
            sb.append(String.format("<h3 class=\"job-url\">%s</h3>", job.getName()));
        }
        sb.append(String.format("<p class=\"job-details\">(for %s - job %s)</p>", job.getUserName(), job.getId()));
        sb.append(String.format("<p class=\"job-logs\" id=\"%s\">", job.getId()));
        sb.append(job.getExplanation()).append("\n");
        sb.append("Logs will appear below as they are generated:" + "\n");
        sb.append("---- Web: Logs start ----" + "\n");
        sb.append("</p>");
        sb.append("</div>");
        return sb.toString();
    }
}
