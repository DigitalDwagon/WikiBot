package dev.digitaldragon.interfaces.api;

import com.google.gson.Gson;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.AbortHelper;
import dev.digitaldragon.interfaces.generic.DokuWikiDumperHelper;
import dev.digitaldragon.interfaces.generic.StatusHelper;
import dev.digitaldragon.interfaces.generic.WikiTeam3Helper;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import io.javalin.Javalin;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;


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
        runCommand(); //POST /api/command
        createJob(); //POST /api/jobs

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
            JSONObject jsonObject = new JSONObject();
            JSONArray runningJobs = new JSONArray();
            for (Job job : JobManager.getActiveJobs()) {
                runningJobs.put(job.getId());
            }
            jsonObject.put("running", runningJobs);

            JSONArray queuedJobs = new JSONArray();
            for (Job job : JobManager.getQueuedJobs()) {
                queuedJobs.put(job.getId());
            }
            jsonObject.put("queued", queuedJobs);
            ctx.result(jsonObject.toString());
        });
    }

    private static void getJob() {
        app.get("/api/jobs/{id}", (ctx) -> {
            String jobId = ctx.pathParam("id");
            Job job = JobManager.get(jobId);
            if (job != null) {
                JSONObject jsonObject = JobManager.getJsonForJob(job);
                jsonObject.put("jobId", jobId);

                ctx.result(jsonObject.toString());
            } else {
                ctx.status(404).result(error("Job not found"));
            }
        });
    }

    private static void runCommand() {
        app.post("/api/command", (ctx) -> {
            JSONObject json = new JSONObject(ctx.body());
            if (!json.has("command") || !json.has("body")) {
                ctx.status(400).result(error("You must include the \"command\" and \"body\" keys in your request."));
            }

            String body = json.getString("body");
            String username = ctx.req().getHeader("X-Platform-User");
            String successMessage;
            successMessage = switch (json.getString("command")) {
                case "mediawikisingle" -> WikiTeam3Helper.beginJob(body, username);
                case "dokuwikisingle" -> DokuWikiDumperHelper.beginJob(body, username);
                case "abort" -> AbortHelper.abortJob(body);
                case "status" -> StatusHelper.getStatus(body);
                default -> null;
            };

            if (successMessage == null) {
                ctx.status(400).result(error("Invalid command"));
            }
            ctx.result(success(successMessage));


        });
    }

    private static void createJob() {
        app.post("/api/jobs", (ctx) -> {
            try {
                JSONObject json = new JSONObject(ctx.body());
                if (!json.has("jobType")) {
                    ctx.status(400).result(error("You must include a valid \"jobType\" in your request. It may be one of: WIKITEAM3"));
                }
                JSONObject clean = new JSONObject(ctx.body());
                clean.remove("jobType");
                String username = ctx.req().getHeader("X-Platform-User");

                if (json.getString("jobType").equalsIgnoreCase("wikiteam3")) {
                    System.out.println(clean);
                    WikiTeam3Args args = new Gson().fromJson(clean.toString(), WikiTeam3Args.class);
                    System.out.println(args.getExplain());
                    System.out.println(args.getUrl());
                    String jobId = UUID.randomUUID().toString();
                    String message = WikiTeam3Helper.beginJob(args, username, jobId);
                    if (message != null) {
                        ctx.status(400).result(error(message));
                        return;
                    }
                    JSONObject response = new JSONObject();
                    response.put("jobId", jobId);
                    response.put("success", true);
                    ctx.result(response.toString());
                    return;
                } else {
                    ctx.status(400).result(error("Invalid job type"));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            ctx.status(500).result("oops");
        });
    }


    private static String error(String message) {
        return new JSONObject().put("error", message).put("success", false).toString();
    }

    private static String success(String message) {
        return new JSONObject().put("success", true).put("message", message).toString();
    }
}
