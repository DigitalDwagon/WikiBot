package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.List;

import static dev.digitaldragon.interfaces.api.JavalinAPI.app;

public class Dashboard {
    public static void register() {
        help();
        index();
        cards();
    }

    public static void help() {
        app.get("/help", (ctx) -> {
            ctx.res().setHeader("Location", "https://wiki.archiveteam.org/index.php/Wikibot");
            ctx.res().setStatus(302);
            ctx.result("");
        });
    }


    @SneakyThrows
    private static void index() {
        InputStream stream = Dashboard.class.getClassLoader().getResourceAsStream("dashboard.html");
        String content = new String(stream.readAllBytes());
        app.get("/", (ctx) -> {
            ctx.res().setContentType("text/html");
            List<Job> queued_jobs = JobManager.getQueuedJobs();
            StringBuilder queued_cards = new StringBuilder();
            for (Job job : queued_jobs) {
                queued_cards.append(getCard(job));
            }
            List<Job> running_jobs = JobManager.getRunningJobs();
            StringBuilder running_cards = new StringBuilder();
            for (Job job : running_jobs) {
                running_cards.append(getCard(job));
            }
            ctx.result(content.replace("{running_count}", String.valueOf(running_jobs.size()))
                    .replace("{queued_count}", String.valueOf(queued_jobs.size()))
                    .replace("{queued_cards}", queued_cards)
                    .replace("{running_cards}", running_cards));
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
        sb.append("<div class=\"job-container");
        sb.append(String.format(" job-status-%s", job.getStatus().toString().toLowerCase()));
        sb.append("\"");
        sb.append(String.format(" id=\"job-container-%s\"", job.getId()));
        sb.append(">");
        JobMeta meta = job.getMeta();
        if (meta.getTargetUrl().isPresent()) {
            String jobUrl = meta.getTargetUrl().get();
            sb.append(String.format("<h3 class=\"job-url\"><a href=\"%s\">%s</a></h3>", jobUrl, jobUrl));
        } else {
            sb.append(String.format("<h3 class=\"job-url\">%s</h3>", "Unknown URL"));
        }
        sb.append("<p class=\"job-details\">");
        sb.append("(");
        if (meta.getUserName().isPresent()) {
            sb.append("for ");
            if (meta.getPlatform().isPresent()) {
                sp.append(String.format("<span title=\"on %s\">", meta.getPlatform()));
            }
            sb.append(meta.getUserName());
            if (meta.getPlatform().isPresent()) {
                sb.append("</span>");
            }
            sb.append(" - ");
        }
        sb.append(String.format("job %s", job.getId()));
        sb.append(")");
        if (meta.getExplain().isPresent()) {
            sb.append(" ");
            sb.append(meta.getExplain().get());
        }
        sb.append("</p>");
        sb.append(String.format("<p class=\"job-logs\" id=\"%s\">", job.getId()));
        sb.append("Logs will appear below as they are generated:" + "\n");
        sb.append("---- Web: Logs start ----" + "\n");
        sb.append("</p>");
        sb.append("</div>");
        return sb.toString();
    }
}
