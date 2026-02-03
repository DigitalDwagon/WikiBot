package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Header;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavalinAPI {
    public static Javalin app;
    public static void register() {
        app = Javalin.create().start(WikiBot.getConfig().getDashboardConfig().port());
        UpdatesWebsocket updatesWebsocket = new UpdatesWebsocket();
        LogWebsocket logWebsocket = new LogWebsocket();

        app.ws("/api/logfirehose", logWebsocket);
        app.ws("/api/jobevents", updatesWebsocket);

        enableCORS("*", "*", "*");
        postValidation(); //validate all POST requests

        Dashboard.register();
        //register routes
        getAllJobs(); //GET /api/jobs
        getJob(); //GET /api/jobs/:id
        //createJob(); //POST /api/jobs

        WikiBot.getBus().register(updatesWebsocket);
        WikiBot.getBus().register(logWebsocket);

        app.get("/api/jobs/{id}/logs", (ctx) -> {
            String jobId = ctx.pathParam("id");
            int limit = -1;
            if (ctx.queryParam("limit") != null) {
                try {
                    limit = Integer.parseInt(ctx.queryParam("limit"));
                } catch (NumberFormatException e) {
                    ctx.status(400);
                    ctx.result(WikiBot.getGson().toJson(new ErrorResponse("Invalid \"limit\" parameter - got " + ctx.queryParam("limit") + ", expected a valid number")));
                    return;
                }
            }

            File jobDir = new File("jobs/" + jobId);
            if (!jobDir.exists() || !jobDir.isDirectory()) {
                ctx.status(404);
                ctx.result(WikiBot.getGson().toJson(new ErrorResponse("Job not found")));
                return;
            }

            File logFile = new File(jobDir, "log.txt");
            if (!logFile.exists()) {
                ctx.result();
                return;
            }

            if (limit < 0) {
                try {
                    ctx.contentType(ContentType.TEXT_PLAIN);
                    ctx.header(Header.CONTENT_DISPOSITION, "inline");
                    ctx.result(Files.newInputStream(logFile.toPath()));
                } catch (IOException e) {
                    e.printStackTrace();
                    ctx.status(500);
                    ctx.result(WikiBot.getGson().toJson(new ErrorResponse("Ran into an error while reading log file: " + e.getMessage())));
                }
                return;
            }

            long start;
            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                long pointer = raf.length() - 1;
                int lines = 0;

                limit++; //since the trailing \n will count as a line
                while (pointer >= 0 && lines < limit) {
                    raf.seek(pointer);
                    int readByte = raf.read();

                    if (readByte == '\n') {
                        lines++;
                    }

                    pointer--;
                }

                // Position is now just before the last 2000 lines
                raf.seek(pointer + 1);

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = raf.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                ctx.contentType(ContentType.TEXT_PLAIN);
                ctx.header(Header.CONTENT_DISPOSITION, "inline");
                ctx.result(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
                ctx.status(500);
                ctx.result(WikiBot.getGson().toJson(new ErrorResponse("Ran into an error while reading log file: " + e.getMessage())));
            }
        });
    }

    private static void enableCORS(final String origin, final String methods, final String headers) {
        app.options("/*", (ctx) -> {
            String accessControlRequestHeaders = ctx.req().getHeader("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                ctx.res().setHeader("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = ctx.req().getHeader("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                ctx.res().setHeader("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            ctx.result("OK");
        });

        app.before((ctx) -> {
            ctx.header("Access-Control-Allow-Origin", origin);
            ctx.header("Access-Control-Request-Method", methods);
            ctx.header("Access-Control-Allow-Headers", headers);
            ctx.contentType("application/json");
            ctx.status(200);
        });
    }

    private static void postValidation() {
        app.before((ctx) -> {
            HttpServletRequest req = ctx.req();
            if (!req.getMethod().equals("POST")) {
                return;
            }
            // require a set username
            if (req.getHeader("X-Platform-User") == null) {
                ctx.status(400);
                ctx.result(error("You must set a username via the X-Platform-User header"));
                return;
            }

            //require a set platform name
            String platform = req.getHeader("X-Platform");
            if (platform == null) {
                ctx.status(400);
                ctx.result(error("You must specify your application via the X-Platform header"));
                return;
            }

            /*String auth = req.getHeader("Authorization");
            if (auth == null) {
                ctx.status(400);
                ctx.result(error("You must specify an API key via the Authorization header"));
                return;
            }
            auth = auth.replaceFirst("Bearer ", "");
            if (!auth.equals(EnvConfig.getConfigs().get("api_key_" + platform))) {
                ctx.status(401);
                ctx.result(error("Invalid API key"));
                return;
            }//*/


            //valid JSON is required
            try {
                JSONObject json = new JSONObject(ctx.body());
            } catch (JSONException e) {
                ctx.status(400);
                ctx.result(error("Invalid JSON"));
                return;
            }


        });
    }

    private static void getAllJobs() {
        app.get("/api/jobs", (ctx) -> {
            if (ctx.queryParam("details") != null && ctx.queryParam("details").equalsIgnoreCase("true")) {
                Map<String, List<Job>> response = new HashMap<>();
                response.put("queued", JobManager.getQueuedJobs());
                response.put("running", JobManager.getRunningJobs());
                ctx.result(WikiBot.getGson().toJson(response));
                return;
            }

            Map<String, List<String>> response = new HashMap<>();
            response.put("queued", JobManager.getQueuedJobs().stream().map(Job::getId).toList());
            response.put("running", JobManager.getRunningJobs().stream().map(Job::getId).toList());
            ctx.result(WikiBot.getGson().toJson(response));
        });
    }

    private static void getJob() {
        app.get("/api/jobs/{id}", (ctx) -> {
            String jobId = ctx.pathParam("id");
            Job job = JobManager.get(jobId);
            if (job != null) {
                ctx.result(WikiBot.getGson().toJson(job));
            } else {
                ctx.status(404).result(error("Job not found"));
            }
        });
    }

    private static String error(String message) {
        return new JSONObject().put("error", message).put("success", false).toString();
    }

    private static String success(String message) {
        return new JSONObject().put("success", true).put("message", message).toString();
    }

    private record ErrorResponse(String error) {}
}
