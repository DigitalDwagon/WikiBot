package dev.digitaldragon.interfaces.api;

import com.google.gson.*;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.api.messages.SystemMessage;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperArgs;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperArgs;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperJob;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class JavalinAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavalinAPI.class);
    private static final Path API_TOKENS_FILE = Path.of("api-tokens.json");
    private static List<String> allowedTokens = new ArrayList<>();

    public static Javalin app;
    public static void register() {
        app = Javalin.create().start(WikiBot.getConfig().getDashboardConfig().port());
        UpdatesWebsocket updatesWebsocket = new UpdatesWebsocket();
        LogWebsocket logWebsocket = new LogWebsocket();

        app.ws("/api/logfirehose", logWebsocket);
        app.ws("/api/jobevents", updatesWebsocket);

        enableCORS("*", "*", "*");
        requireApiToken();

        try {
            allowedTokens = loadApiTokens();
        } catch (IOException | JsonParseException | IllegalStateException  e) {
            LOGGER.error("Failed to load API tokens from {}", API_TOKENS_FILE, e);
        }

        Dashboard.register();
        //register routes
        getAllJobs(); //GET /api/jobs
        postJob(); //POST /api/jobs
        getJob(); //GET /api/jobs/:id
        postReloadTokens(); //POST /api/reload-tokens

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

        app.get("/api/system", (ctx) -> {
            File file = new File("jobs");
            long freeSpace = file.getFreeSpace();
            long totalSpace = file.getTotalSpace();

            String json = WikiBot.getGson().toJson(new SystemMessage(new SystemMessage.DiskSpaceInfo(freeSpace, totalSpace)));
            ctx.result(json);
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

    private static void requireApiToken() {
        app.before((ctx) -> {
            if (!ctx.method().name().equals("POST")) {
                return;
            }

            String token = bearerToken(ctx.header("Authorization"));
            if (token == null) {
                ctx.status(401).result(error("Missing or invalid Bearer token"));
                ctx.skipRemainingHandlers();
                return;
            }

            boolean authorized = false;
            for (String allowedToken : allowedTokens) {
                authorized |= constantTimeEquals(token, allowedToken);
            }
            if (!authorized) {
                ctx.status(401).result(error("Invalid Bearer token"));
                ctx.skipRemainingHandlers();
            }
        });
    }

    private static String bearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }

        String[] parts = authorization.trim().split("\\s+", 2);
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("Bearer")
                || parts[1].isBlank() || parts[1].chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        return parts[1];
    }

    private static boolean constantTimeEquals(String first, String second) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] firstHash = digest.digest(first.getBytes(StandardCharsets.UTF_8));
            byte[] secondHash = digest.digest(second.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(firstHash, secondHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void postJob() {
        app.post("/api/jobs", (ctx) -> {
            JsonElement requestBody;
            try {
                requestBody = JsonParser.parseString(ctx.body());
            } catch (JsonParseException e) {
                ctx.status(400).result(error("Invalid JSON"));
                return;
            }

            if (!requestBody.isJsonObject()) {
                ctx.status(400).result(error("Request body must be a JSON object"));
                return;
            }

            JsonObject request = requestBody.getAsJsonObject();
            JsonElement typeElement = request.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive() || !typeElement.getAsJsonPrimitive().isString()) {
                ctx.status(400).result(error("Missing or invalid \"type\""));
                return;
            }

            JobType type;
            try {
                type = JobType.valueOf(typeElement.getAsString());
            } catch (IllegalArgumentException e) {
                ctx.status(400).result(error("Unknown job type: " + typeElement.getAsString()));
                return;
            }

            JsonElement argsElement = request.get("args");
            if (argsElement == null || !argsElement.isJsonObject()) {
                ctx.status(400).result(error("\"args\" must be a JSON object"));
                return;
            }

            JsonElement metaElement = request.get("meta");
            if (metaElement == null || !metaElement.isJsonObject()) {
                ctx.status(400).result(error("\"meta\" must be a JSON object"));
                return;
            }

            JobMeta meta;
            try {
                meta = WikiBot.getGson().fromJson(metaElement, JobMeta.class);
            } catch (JsonParseException e) {
                ctx.status(400).result(error("Invalid \"meta\" object: " + e.getMessage()));
                return;
            }

            if (meta == null || meta.getUserName() == null || meta.getUserName().isBlank()) {
                ctx.status(400).result(error("\"meta.userName\" is required"));
                return;
            }
            if (meta.getPlatform() == null) {
                ctx.status(400).result(error("\"meta.platform\" is required and must be a valid JobPlatform"));
                return;
            }

            Job job;
            try {
                job = switch (type) {
                    case WIKITEAM3 -> new WikiTeam3Job(WikiBot.getGson().fromJson(argsElement, WikiTeam3Args.class), meta);
                    case DOKUWIKIDUMPER -> new DokuWikiDumperJob(WikiBot.getGson().fromJson(argsElement, DokuWikiDumperArgs.class), meta);
                    case PUKIWIKIDUMPER -> new PukiWikiDumperJob(WikiBot.getGson().fromJson(argsElement, PukiWikiDumperArgs.class), meta);
                    case REUPLOAD -> {
                        JsonElement targetId = argsElement.getAsJsonObject().get("targetId");
                        if (targetId == null || !targetId.isJsonPrimitive() || !targetId.getAsJsonPrimitive().isString()) {
                            throw new IllegalStateException("Invalid or missing \"targetId\" for Reupload job");
                        }

                        yield new ReuploadJob(meta, UUID.randomUUID().toString(), targetId.getAsString());
                    }
                    default -> throw new IllegalStateException("Unsupported job type: " + type);
                };
            } catch (JsonParseException e) {
                ctx.status(400).result(error("Invalid \"args\" object: " + e.getMessage()));
                return;
            } catch (JobLaunchException | IllegalStateException e) {
                ctx.status(400).result(error(e.getMessage()));
                return;
            }

            JobManager.submit(job);
            ctx.status(201).result(WikiBot.getGson().toJson(job));
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

    private static void postReloadTokens() {
        app.post("/api/reload-tokens", (ctx) -> {
            List<String> allowedTokens;
            try {
                allowedTokens = loadApiTokens();
            } catch (IOException | JsonParseException | IllegalStateException e) {
                LOGGER.error("Failed to load API tokens from {}", API_TOKENS_FILE, e);
                ctx.status(500).result(error("API authentication is not configured correctly"));
                return;
            }
            JavalinAPI.allowedTokens = allowedTokens;
            ctx.status(200).result(WikiBot.getGson().toJson(new APIResponse(true, "Successfully reloaded API tokens", null)));
        });
    }

    private static List<String> loadApiTokens() throws IOException {
        JsonElement document = JsonParser.parseString(Files.readString(API_TOKENS_FILE));
        if (!document.isJsonObject()) {
            throw new IllegalStateException("API token document must be a JSON object");
        }

        JsonElement tokensElement = document.getAsJsonObject().get("tokens");
        if (tokensElement == null || !tokensElement.isJsonArray()) {
            throw new IllegalStateException("API token document must contain a tokens array");
        }

        JsonArray tokensArray = tokensElement.getAsJsonArray();
        List<String> tokens = new java.util.ArrayList<>(tokensArray.size());
        for (JsonElement element : tokensArray) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Each API token must be a JSON object");
            }

            JsonObject tokenObject = element.getAsJsonObject();
            JsonElement tokenElement = tokenObject.get("token");
            JsonElement commentElement = tokenObject.get("comment");
            if (tokenElement == null || !tokenElement.isJsonPrimitive()
                    || !tokenElement.getAsJsonPrimitive().isString() || tokenElement.getAsString().isBlank()
                    || (commentElement != null && (!commentElement.isJsonPrimitive()
                    || !commentElement.getAsJsonPrimitive().isString()))) {
                throw new IllegalStateException("Each API token requires a non-empty token and optional string comment");
            }
            tokens.add(tokenElement.getAsString());
        }
        return tokens;
    }

    private static String error(String message) {
        return WikiBot.getGson().toJson(new APIResponse(false, null, message));
    }

    private record ErrorResponse(String error) {}
    private record APIResponse(boolean success, String message, String error) {}
}
