package dev.digitaldragon.archive;

import dev.digitaldragon.Main;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DokuWikiArchive {
    public static final String DUMPS_DIRECTORY = "dumps/";
    public static void ArchiveWiki(String url, String note, User user, ThreadChannel channel, String options) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Main.getExecutorService().submit(() -> {
            try {
                File dumpsDir = new File(DUMPS_DIRECTORY);

                String command = "dokuwikidumper " + options + url;
                System.out.println(command);

                ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                processBuilder.directory(dumpsDir);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

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

                        // Log to discord every 25 lines or 60 seconds
                        if (logs.size() % 25 == 0 || System.currentTimeMillis() - flushTime >= 60000) {
                            //copy of logs
                            final List<String> logsToSend = new ArrayList<>(logs);
                            executorService.submit(() -> sendLogs(channel, logsToSend));
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
                    sendLogs(channel, logs);
                }

                channel.sendMessage("Process ended. " + user.getAsMention()).queue();
                channel.sendMessage("Note: ```" + note + "```").queue();


                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    channel.sendMessage(user.getAsMention() + " Command failed with exit code " + exitCode).queue();
                }
            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage(e.getMessage()).queue();
            } finally {
                executorService.shutdown();
            }
        });
    }

    private void readStreamAndHandleLogs(InputStream stream, List<String> logs, String url, long flushTime, ThreadChannel channel) {
        try (BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = streamReader.readLine()) != null) {
                logs.add(line);
                System.out.println(line + " | for: " + url);

                // Call showProgressBars every 25 lines or 60 seconds
                if (logs.size() % 25 == 0 || System.currentTimeMillis() - flushTime >= 60000) {
                    sendLogs(channel, logs);
                    logs.clear();
                    flushTime = System.currentTimeMillis();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void writeToFile(String filename, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void sendLogs(ThreadChannel channel, List<String> logs) {
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("```");

        for (String messageText : logs) {
            messageBuilder.append(messageText).append("\n");
        }

        messageBuilder.append("```");

        String messageContent = messageBuilder.toString().trim();
        if (!messageContent.isEmpty()) {
            channel.sendMessage(messageContent).queue();
        }
    }
}
