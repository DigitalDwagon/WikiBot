package dev.digitaldragon.archive;

import com.google.gson.JsonObject;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.util.*;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The DokuWikiArchive class provides methods to archive a DokuWiki wiki using DokuWikiDumper, logging to Discord and uploading the results to archive.org.
 */


public class RunJob {
    static String archiveUrl = "";
    /**
     * Runs an archiving job, sending the logs to a channel and saving them to a log file.
     *
     * @param jobName     name used to refer to the job being run
     * @param note        a note to include in the archive process
     * @param userMention the user initiating the archive process
     * @param userName    the name of the user initiating the archive process
     * @param channel     the thread channel to send the logs and notifications to
     * @param jobId       the ID of the job associated with the archive process
     * @param tasks       tasks to perform during the archive process
     * @throws IllegalArgumentException if no tasks are provided
     */
    public static void startArchive(String jobName, String note, String userMention, String userName, ThreadChannel channel, String jobId, AfterTask afterTask, CommandTask... tasks) {
        //if (tasks.length < 1)
        //    throw new IllegalArgumentException();

        WikiBot.getExecutorService().submit(() -> {
            try {
                File workingDir = createWorkingDirectory(jobId);

                PriorityQueue<CommandTask> queue = new PriorityQueue<>(Arrays.asList(tasks));

                boolean success = true;
                String failingTask = null;
                int failCode = 0;
                while (!queue.isEmpty()) {
                    CommandTask task = queue.poll();
                    String command = task.getCommand();
                    String taskName = task.getName();

                    System.out.println(command);


                    sendLogs(channel,
                            List.of("",
                                    "----- Bot: Starting Task: " + taskName + " -----",
                                    "----- Bot: Command: " + command + " -----",
                                    "")
                            , String.format("jobs/%s/log.txt", jobId));

                    ProcessBuilder processBuilder = getExecutingProcess(command, workingDir);
                    Process process = handleLogs(processBuilder, channel, jobId);
                    System.out.println("Process ended. For: " + taskName);
                    int exitCode = 999;

                    if (process != null)
                        exitCode = process.waitFor();

                    sendLogs(channel,
                            List.of("",
                                    "----- Bot: Finished Task: " + taskName + " -----",
                                    "----- Bot: Task exit code: " + exitCode + " -----",
                                    "")
                            , String.format("jobs/%s/log.txt", jobId));

                    if (!task.taskSuccess(exitCode)) {
                        success = false;
                        if (failingTask == null) {
                            failingTask = taskName;
                            failCode = exitCode;
                        }
                        break;
                    }
                }

                /*if (afterTask == AfterTask.MEDIAWIKI && success) { //todo we should be able to remove this when upstream is fixed :D
                    sendLogs(channel,
                            List.of("----- Bot: Starting Task: IAUpload Pseudo Task -----")
                            , String.format("jobs/%s/log.txt", jobId));
                    try {
                        for (File file : workingDir.listFiles()) {
                            if (!file.isDirectory()) {
                                continue;
                            }
                            // upload wikidump.7z and history.7z to archive.org
                            JSONObject metadata;
                            try {
                                InputStream input = new FileInputStream(file.getPath() + "/siteinfo.json");
                                String jsonText = IOUtils.toString(input, StandardCharsets.UTF_8);
                                JSONObject siteInfo = new JSONObject(jsonText);
                                JSONObject query = siteInfo.getJSONObject("query");
                                metadata = query.getJSONObject("general");
                            } catch (Exception e) {
                                sendLogs(channel, List.of("Bot: Failed to read siteinfo.json"), String.format("jobs/%s/log.txt", jobId));
                                failingTask = "IAUpload";
                                failCode = 9;
                                metadata = new JSONObject();
                            }
                            String identifier = "wiki-" + file.getName();
                            archiveUrl = "https://archive.org/details/" + identifier;
                            String siteName = metadata.getString("sitename");
                            String title = "Wiki: " + siteName;
                            String originalUrl = metadata.getString("base");
                            String description = String.format("<a href=\\\"%s\\\" rel=\\\"nofollow\\\">%s</a> dumped with <a href=\\\"https://github.com/mediawiki-client-tools/mediawiki-scraper\\\"rel=\\\"nofollow\\\">WikiTeam3</a> via WikiBot.",
                                    originalUrl, siteName);
                            String subject = "wiki;wikiteam;WikiBot;MediaWiki;wikiteam3;wikidump";

                            StringBuilder iaCommand = new StringBuilder("ia upload ");
                            iaCommand.append(identifier).append(" ");
                            iaCommand.append(file.getAbsolutePath()).append("/").append("wikidump.7z").append(" ");
                            iaCommand.append(file.getAbsolutePath()).append("/").append("history.7z").append(" ");
                            iaCommand.append("--metadata=\"title:").append(title).append("\" ");
                            iaCommand.append("--metadata=\"description:").append(description).append("\" ");
                            iaCommand.append("--metadata=\"originalurl:").append(originalUrl).append("\" ");
                            iaCommand.append("--metadata=\"subject:").append(subject).append("\" ");
                            iaCommand.append("--metadata=\"mediatype:web\"");

                            String commandString = iaCommand.toString();
                            System.out.println(commandString);


                            sendLogs(channel,
                                    List.of("----- Bot: Starting Task: IAUpload Pseudo Task -----")
                                    , String.format("jobs/%s/log.txt", jobId));

                            ProcessBuilder processBuilder = getExecutingProcess(commandString, workingDir);
                            Process process = handleLogs(processBuilder, channel, jobId);
                            System.out.println("Process ended. For: IAUpload Pseudo Task");
                            int exitCode = 999;

                            if (process != null)
                                exitCode = process.waitFor();

                            if (exitCode != 0) {
                                success = false;
                                failCode = exitCode;
                                failingTask = "IAUpload Pseudo Task";
                            }

                            sendLogs(channel,
                                    List.of("----- Bot: Finishing Task: IAUpload Pseudo Task -----")
                                    , String.format("jobs/%s/log.txt", jobId));
                        }
                    } catch (Exception e) {
                        sendLogs(channel, List.of("Bot: Failed in IAUpload!"), String.format("jobs/%s/log.txt", jobId));
                        failingTask = "IAUpload Pseudo Task";
                        success = false;
                        failCode = 999;
                    }

                }*/

                if ((afterTask == AfterTask.MEDIAWIKI || afterTask == AfterTask.DOKUWIKI ) && success) { //todo still no --upload :(
                    sendLogs(channel,
                            List.of("----- Bot: Starting Task: Uploader Pseudo Task -----")
                            , String.format("jobs/%s/log.txt", jobId));
                    try {
                        for (File file : workingDir.listFiles()) {
                            if (!file.isDirectory()) {
                                continue;
                            }

                            String commandString = "";
                            if (afterTask == AfterTask.MEDIAWIKI)
                                commandString = "wikiteam3uploader " + file.getName() + " --zstd-level 22";

                            if (afterTask == AfterTask.DOKUWIKI)
                                commandString = "dokuWikiUploader " + file.getName();


                            sendLogs(channel,
                                    List.of("----- Bot: Starting Task: Uploader Pseudo Task -----")
                                    , String.format("jobs/%s/log.txt", jobId));

                            ProcessBuilder processBuilder = getExecutingProcess(commandString, workingDir);
                            Process process = handleLogs(processBuilder, channel, jobId);
                            System.out.println("Process ended. For: Uploader Pseudo Task");
                            int exitCode = 999;

                            if (process != null)
                                exitCode = process.waitFor();

                            if (exitCode != 0) {
                                success = false;
                                failCode = exitCode;
                                failingTask = "Uploader Pseudo Task";
                            }

                            sendLogs(channel,
                                    List.of("----- Bot: Finishing Task: Uploader Pseudo Task -----")
                                    , String.format("jobs/%s/log.txt", jobId));
                        }
                    } catch (Exception e) {
                        sendLogs(channel, List.of("Bot: Failed in Uploader!"), String.format("jobs/%s/log.txt", jobId));
                        failingTask = "IAUpload Pseudo Task";
                        success = false;
                        failCode = 999;
                    }

                }

                System.out.println(archiveUrl);

                UploadObject.uploadObject("digitaldragons", "cdn.digitaldragon.dev", "wikibot/jobs/" + jobId + "/log.txt", String.format("jobs/%s/log.txt", jobId), "text/plain; charset=utf-8", "inline");

                String logsUrl = String.format("https://cdn.digitaldragon.dev/wikibot/jobs/%s/log.txt", jobId);
                channel.sendMessage("Job ended.").queue();
                channel.sendMessage("Note: ```" + note + "```").queue();
                channel.sendMessage("Logs are available at " + logsUrl).queue();

                if (success) {
                    TextChannel successChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_success_channel"));
                    if (successChannel != null)
                        successChannel.sendMessage(String.format("%s for %s:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nArchive URL: %s\nNote: ```%s```", jobName, userMention, channel.getAsMention(), logsUrl, jobId, archiveUrl, note)).queue();

                    IRCClient.sendMessage(userName, "Success! Job " + jobId + " completed successfully.");
                    IRCClient.sendMessage("Archive URL: " + archiveUrl);

                } else {
                    TextChannel failChannel = WikiBot.getInstance().getTextChannelById(EnvConfig.getConfigs().get("discord_failure_channel"));
                    if (failChannel != null)
                        failChannel.sendMessage(String.format("%s for %s:\n\nThread: %s \nLogs: %s\nJob ID: `%s`\nFailed Task: `%s`\nExit Code: `%s`\nNote: ```%s```", jobName, userMention, channel.getAsMention(), logsUrl, jobId, failingTask, failCode, note)).queue();
                    channel.sendMessage("Task indicated as failed.").queue();
                    IRCClient.sendMessage(userName, String.format("Job %s failed on task %s with exit code %s.", jobId, failingTask, failCode));


                }
                IRCClient.sendMessage("Explanation: " + note);
                IRCClient.sendMessage("Logs: " + logsUrl);

            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage(e.getMessage()).queue();
            }
        });
    }

    /**
     * Handles the logs generated by a process, sending them to a channel and saving them to a log file.
     *
     * @param processBuilder  the processbuilder to execute
     * @param channel  the thread channel to send the logs to
     * @param jobId    the id of the job associated with the logs
     */
    public static Process handleLogs(ProcessBuilder processBuilder, ThreadChannel channel, String jobId) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            List<String> logs = new ArrayList<>();
            long flushTime = System.currentTimeMillis();

            StringBuilder logBuilder = new StringBuilder();
            int c;

            while ((c = reader.read()) != -1) {
                char character = (char) c;

                // Handle both '\n' and '\r' as newline characters
                if (character == '\n' || character == '\r') {
                    String logLine = logBuilder.toString(); // Get the completed line
                    logs.add(logLine);
                    System.out.println(logLine);
                    logBuilder = new StringBuilder(); // Reset the StringBuilder for the next line

                    //if the line contains the string "https://archive.org/details/" then it is the archive url, extract it from the line and save it
                    if (logLine.contains("https://archive.org/details/") && logLine.contains(" ")) {
                        String[] split = logLine.split(" ");
                        for (String s : split) {
                            if (s.contains("https://archive.org/details/")) {
                                archiveUrl = s;
                                break;
                            }
                        }
                    } else if (logLine.contains("https://archive.org/details/")) {
                        archiveUrl = logLine;
                    }


                    // Log to discord every 35 lines or 60 seconds
                    if (logs.size() % 35 == 0 || System.currentTimeMillis() - flushTime >= 60000) {
                        //copy of logs
                        final List<String> logsToSend = new ArrayList<>(logs);
                        executorService.submit(() -> sendLogs(channel, logsToSend, String.format("jobs/%s/log.txt", jobId)));
                        //clear logs
                        logs.clear();
                        flushTime = System.currentTimeMillis();
                    }
                } else {
                    logBuilder.append(character); // Add the character to the current line
                }
            }

            // After the loop, check if there's an incomplete line to add
            if (!logBuilder.isEmpty()) {
                String logLine = logBuilder.toString();
                logs.add(logLine);
            }

            if (!logs.isEmpty()) {
                sendLogs(channel, logs, String.format("jobs/%s/log.txt", jobId));
            }
            return process;
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            executorService.shutdown();
        }
        return null;
    }


    /**
     * Creates a process to execute a command in Powershell or Bash based on operating system.
     *
     * @param command  the command to be executed
     * @param dumpsDir the directory where the process will be executed
     * @return the executing process
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if the operating system is not supported
     */
    @NotNull
    public static ProcessBuilder getExecutingProcess(String command, File dumpsDir) throws IOException {
        ProcessBuilder processBuilder;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) { // For Windows
            processBuilder = new ProcessBuilder("powershell.exe", "/c", command);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) { //For Unix-based
            processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }
        processBuilder.directory(dumpsDir);
        processBuilder.redirectErrorStream(true);

        return processBuilder;
    }

    /**
     * Sends logs to a specified thread channel.
     *
     * @param channel   the thread channel to send the logs to
     * @param logs      the list of logs to send
     * @param filename  the name of the file to write the logs to
     */
    public static void sendLogs(ThreadChannel channel, List<String> logs, String filename) {
        StringBuilder messageBuilder = new StringBuilder();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            messageBuilder.append("```");

            for (String messageText : logs) {
                messageBuilder.append(messageText).append("\n");
                writer.write(messageText);
                writer.newLine();
            }

            messageBuilder.append("```");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String messageContent = messageBuilder.toString().trim();
        if (!messageContent.isEmpty()) {
            try {
                channel.sendMessage(messageContent).queue();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    channel.sendMessage(e.getMessage()).queue();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

    }

    /**
     * Creates a work directory for a specified job ID.
     *
     * @param jobId  the ID of the job to create the log file for
     * @throws IOException when creation fails
     */
    public static File createWorkingDirectory(String jobId) throws IOException {
        String logFilePath = String.format("jobs/%s/log.txt", jobId);
        File logFile = new File(logFilePath);

        // Ensure jobs/ exists
        File jobsDir = new File("jobs");
        if (!jobsDir.exists()) {
            boolean dirCreated = jobsDir.mkdirs();
            if (!dirCreated) {
                throw new IOException("Failed to create directory: " + jobsDir);
            }
        }

        // Ensure the parent (logs/jobid) directory exists
        File parentDir = logFile.getParentFile();
        if (!parentDir.exists()) {
            boolean dirCreated = parentDir.mkdirs();
            if (!dirCreated) {
                throw new IOException("Failed to create directory: " + parentDir);
            }
        }

        // Ensure the logs/jobid/log.txt file exists
        if (!logFile.exists()) {
            boolean fileCreated = logFile.createNewFile();;
            if (!fileCreated) {
                throw new IOException("Failed to create file: " + logFile);
                // Handle error - e.g., throw an exception, exit, etc.
            }
        }

        return parentDir;
    }
}
