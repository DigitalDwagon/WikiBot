package dev.digitaldragon;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final String FAILED_FILE = "failed.txt";
    private static final String FINISHED_FILE = "done.txt";
    private static final String TODO_FILE = "todo.txt";
    private static final int MAX_CONCURRENT_DUMPS = 5;
    private static final String DUMPS_DIRECTORY = "dumps/";

    public static void main(String[] args) {
        // Read the URLs from the todo.txt file
        List<String> urls = readUrlsFromFile(TODO_FILE);

        // Execute the dokuwikidumper commands concurrently
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_DUMPS);
        for (String url : urls) {
            executorService.execute(() -> runDokuWikiDumperCommand(url));
        }

        // Shutdown the executor service
        executorService.shutdown();
    }

    private static List<String> readUrlsFromFile(String filename) {
        List<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String url = line.trim();
                urls.add(url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urls;
    }

    private static void runDokuWikiDumperCommand(String url) {
        try {
            File dumpsDir = new File(DUMPS_DIRECTORY);
            Process process = Runtime.getRuntime().exec("dokuwikidumper --auto -u --ignore-disposition-header-missing " + url, null, dumpsDir);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String lastLine = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line + " for: " + url);
                lastLine = line;
            }

            String archiveLink = extractArchiveLink(lastLine);
            if (archiveLink != null) {
                System.out.println("Archive.org link: " + archiveLink + " for: " + url);
                writeToFile(FINISHED_FILE, url + " " + archiveLink);
            } else {
                System.out.println("No archive.org link found for: " + url);
                writeToFile(FAILED_FILE, url);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Command execution failed with exit code: " + exitCode + " for: " + url);
                writeToFile(FAILED_FILE, url);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            writeToFile(FAILED_FILE, url);
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

    private static synchronized void writeToFile(String filename, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}