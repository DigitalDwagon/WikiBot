package dev.digitaldragon.archive;

import dev.digitaldragon.ArchiveBot;
import dev.digitaldragon.util.UploadObject;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DokuWikiArchive {
    public static final String DUMPS_DIRECTORY = "dumps/";
    public static void ArchiveWiki(String url, String note, User user, ThreadChannel channel, String options, String jobId) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ArchiveBot.getExecutorService().submit(() -> {
            try {
                File dumpsDir = new File(DUMPS_DIRECTORY);
                createLogsFile(jobId);

                String command = "dokuWikiDumper " + options + url;
                System.out.println(command);

                String os = System.getProperty("os.name").toLowerCase();
                Process process = getExecutingProcess(os, command, dumpsDir);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                List<String> logs = new ArrayList<>();
                long flushTime = System.currentTimeMillis();

                StringBuilder logBuilder = new StringBuilder();
                int c;

                while ((c = reader.read()) != -1 && process.isAlive()) {
                    char character = (char) c;

                    // Handle both '\n' and '\r' as newline characters
                    if (character == '\n' || character == '\r') {
                        String logLine = logBuilder.toString(); // Get the completed line
                        logs.add(logLine);
                        System.out.println(logLine + " | for: " + url);
                        logBuilder = new StringBuilder(); // Reset the StringBuilder for the next line

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

                System.out.println("Process ended. For: " + url);

                if (!logs.isEmpty()) {
                    sendLogs(channel, logs, String.format("jobs/%s/log.txt", jobId));
                }
                UploadObject.uploadObject("digitaldragons", "cdn.digitaldragon.dev", "dokuwikiarchiver/jobs/" + jobId + "/log.txt", String.format("jobs/%s/log.txt", jobId), "text", "inline");

                String logsUrl = String.format("https://cdn.digitaldragon.dev/dokuwikiarchiver/jobs/%s/log.txt", jobId);
                channel.sendMessage("Process ended.").queue();
                channel.sendMessage("Note: ```" + note + "```").queue();
                channel.sendMessage("Logs are available at " + logsUrl).queue();


                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    TextChannel successChannel = ArchiveBot.getInstance().getTextChannelById("1127417094930169918");
                    if (successChannel != null)
                        successChannel.sendMessage(String.format("<%s> for %s:\n\nThread: %s\nLogs: %s\nJob ID: `%s`\nNote: ```%s```", url, user.getAsMention(), channel.getAsMention(), logsUrl, jobId, note)).queue();

                } else {
                    TextChannel failChannel = ArchiveBot.getInstance().getTextChannelById("1127440691602141184");
                    if (failChannel != null)
                        failChannel.sendMessage(String.format("<%s> for %s:\n\nThread: %s \nLogs: %s\nJob ID: `%s`\nExit Code: `%s`\nNote: ```%s```", url, user.getAsMention(), channel.getAsMention(), logsUrl, jobId, exitCode, note)).queue();
                    channel.sendMessage("Command failed with exit code " + exitCode).queue();
                }

            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage(e.getMessage()).queue();
            } finally {
                executorService.shutdown();
            }
        });
    }

    @NotNull
    private static Process getExecutingProcess(String os, String command, File dumpsDir) throws IOException {
        ProcessBuilder processBuilder;

        if (os.contains("win")) { // For Windows
            processBuilder = new ProcessBuilder("powershell.exe", "/c", command);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) { //For Unix-based
            processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }
        processBuilder.directory(dumpsDir);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        return process;
    }

    private static String extractArchiveLink(String line) {
        if (line != null) {
            String[] words = line.split("\\s+");
            for (String word : words) {
                if (word.startsWith("https://archive.org/details/")) {
                    return word;
                }
            }
        }
        return null;
    }

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
            channel.sendMessage(messageContent).queue();
        }

    }

    private static void createLogsFile(String jobId) {
        String logFilePath = String.format("jobs/%s/log.txt", jobId);
        File logFile = new File(logFilePath);

        // Ensure the parent (logs/jobid) directory exists
        File parentDir = logFile.getParentFile();
        if (!parentDir.exists()) {
            boolean dirCreated = parentDir.mkdirs();
            if (!dirCreated) {
                System.out.println("Failed to create directory: " + parentDir);
                // Handle error - e.g., throw an exception, exit, etc.
            }
        }

        // Ensure the logs/jobid/log.txt file exists
        if (!logFile.exists()) {
            boolean fileCreated = false;
            try {
                fileCreated = logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!fileCreated) {
                System.out.println("Failed to create file: " + logFile);
                // Handle error - e.g., throw an exception, exit, etc.
            }
        }
    }
}
