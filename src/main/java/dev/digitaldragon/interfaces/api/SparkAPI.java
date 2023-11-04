package dev.digitaldragon.interfaces.api;

import com.google.gson.Gson;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.AbortHelper;
import dev.digitaldragon.interfaces.generic.DokuWikiDumperHelper;
import dev.digitaldragon.interfaces.generic.StatusHelper;
import dev.digitaldragon.interfaces.generic.WikiTeam3Helper;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.wikiteam.WikiTeam3Args;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Spark;

import java.util.UUID;

import static spark.Spark.*;

public class SparkAPI {
    public static void register() {
        webSocket("/api/logfirehose", DashboardWebsocket.class);
        Spark.port(4567);
        enableCORS("*", "*", "*");

        //register routes
        getAllJobs(); //GET /api/jobs
        getJob(); //GET /api/jobs/:id
        runCommand(); //POST /api/command
        createJob(); //POST /api/jobs

    }

    private static void enableCORS(final String origin, final String methods, final String headers) {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            response.type("application/json");
            response.status(200);
        });
    }

    private static void postValidation() {
        before((req, res) -> {
            if (!req.requestMethod().equals("POST")) {
                return;
            }
            // require a set username
            if (req.headers("X-Platform-User") == null) {
                halt(400, error("You must set a username via the X-Platform-User header"));
            }

            //require a set platform name
            if (req.headers("X-Platform") == null) {
                halt(400, error("You must specify your application via the X-Platform header"));
            }

            //valid JSON is required
            try {
                JSONObject json = new JSONObject(req.body());
            } catch (JSONException e) {
                halt(400, error("Invalid JSON"));
            }

            //TODO API Auth check

        });
    }

    private static void getAllJobs() {
        get("/api/jobs", (req, res) -> {
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
            res.status(200);
            return jsonObject.toString();
        });
    }

    private static void getJob() {
        get("/api/jobs/:id", (req, res) -> {
            String jobId = req.params(":id");
            Job job = JobManager.get(jobId);
            if (job != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("jobId", jobId);
                jsonObject.put("status", job.getStatus());
                jsonObject.put("explanation", job.getExplanation());
                jsonObject.put("user", job.getUserName());
                jsonObject.put("started", job.getStartTime());
                jsonObject.put("name", job.getName());
                jsonObject.put("runningTask", job.getRunningTask());
                //jsonObject.put("directory", job.getDirectory());
                jsonObject.put("failedTaskCode", job.getFailedTaskCode());
                jsonObject.put("threadChannel", job.getThreadChannel().getId());
                jsonObject.put("archiveUrl", job.getArchiveUrl());
                jsonObject.put("type", job.getType());
                jsonObject.put("isRunning", job.isRunning());
                jsonObject.put("allTasks", job.getAllTasks());
                jsonObject.put("logsUrl", job.getLogsUrl());


                res.status(200);
                return jsonObject.toString();
            } else {
                res.status(404);
                return error("Job not found");
            }
        });
    }

    private static void runCommand() {
        post("/api/command", (req, res) -> {
            JSONObject json = new JSONObject(req.body());
            if (!json.has("command") || !json.has("body")) {
                res.status(400);
                return error("You must include the \"command\" and \"body\" keys in your request.");
            }

            String body = json.getString("body");
            String username = req.headers("X-Platform-User");
            String successMessage;
            try {
                successMessage = switch (json.getString("command")) {
                    case "mediawikisingle" -> WikiTeam3Helper.beginJob(body, username);
                    case "dokuwikisingle" -> DokuWikiDumperHelper.beginJob(body, username);
                    case "abort" -> AbortHelper.abortJob(body);
                    case "status" -> StatusHelper.getStatus(body);
                    default -> null;
                };
            } catch (UserErrorException exception) {
                successMessage = exception.getMessage(); //API was used correctly, even though the user on the other side may have had an error
            }

            if (successMessage == null) {
                res.status(400);
                return error("Invalid command");
            }
            return success(successMessage);


        });
    }

    private static void createJob() {
        post("/api/jobs", (req, res) -> {
            try {
                JSONObject json = new JSONObject(req.body());
                if (!json.has("jobType")) {
                    res.status(400);
                    return error("You must include a valid \"jobType\" in your request. It may be one of: WIKITEAM3");
                }
                JSONObject clean = new JSONObject(req.body());
                clean.remove("jobType");
                String username = req.headers("X-Platform-User");

                try {
                    if (json.getString("jobType").equalsIgnoreCase("wikiteam3")) {
                        System.out.println(clean);
                        WikiTeam3Args args = new Gson().fromJson(clean.toString(), WikiTeam3Args.class);
                        System.out.println(args.getExplain());
                        System.out.println(args.getUrl());
                        String jobId = UUID.randomUUID().toString();
                        String message = WikiTeam3Helper.beginJob(args, username, jobId);
                        if (message != null) {
                            res.status(400);
                            return error(message);
                        }
                        JSONObject response = new JSONObject();
                        response.put("jobId", jobId);
                        response.put("success", true);
                        return response.toString();
                    } else {
                        res.status(400);
                        return error("Invalid or unknown job type.");
                    }
                } catch (UserErrorException exception) {
                    res.status(400);
                    return error(exception.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return "oops";
        });
    }


    private static String error(String message) {
        return new JSONObject().put("error", message).put("success", false).toString();
    }

    private static String success(String message) {
        return new JSONObject().put("success", true).put("message", message).toString();
    }
}
