package dev.digitaldragon.archive;

import dev.digitaldragon.DokuWikiDumperBot;
import dev.digitaldragon.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DokuWikiArchive {
    public static final String DUMPS_DIRECTORY = "dumps/";
    public static void ArchiveWiki(String url, String note, User user, ThreadChannel channel) {
        Main.getExecutorService().submit(() -> {
            try {
                File dumpsDir = new File(DUMPS_DIRECTORY);
                Process process = Runtime.getRuntime().exec("dokuwikidumper --auto -u --ignore-disposition-header-missing " + url, null, dumpsDir);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));


                List<String> logs = new ArrayList<>();
                long flushTime = System.currentTimeMillis();
                String line;
                String lastLine = null;

                while (((line = reader.readLine()) != null || (line = errorReader.readLine()) != null) && process.isAlive()) {
                    logs.add(line);
                    lastLine = line;

                    if (!process.isAlive()) {
                        break;
                    }

                    // Call showProgressBars every 25 lines or 60 seconds
                    if (logs.size() % 25 == 0 || System.currentTimeMillis() - flushTime >= 60000) {
                        sendLogs(channel, logs);
                        logs.clear();
                        flushTime = System.currentTimeMillis();

                    }
                }

                if (logs.size() > 0) {
                    sendLogs(channel, logs);
                }

                channel.sendMessage("Process ended. " + user.getAsMention()).queue();
                channel.sendMessage("Note: ```" + note + "```").queue();


                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    channel.sendMessage(user.getAsMention() + " Command failed with exit code " + exitCode).queue();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                channel.sendMessage(e.getMessage()).queue();
            }
        });
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
