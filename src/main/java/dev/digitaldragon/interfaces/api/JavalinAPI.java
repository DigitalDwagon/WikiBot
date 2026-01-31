package dev.digitaldragon.interfaces.api;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import io.javalin.Javalin;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;

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
}
