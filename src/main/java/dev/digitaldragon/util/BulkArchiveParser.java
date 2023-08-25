package dev.digitaldragon.util;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BulkArchiveParser {
    public static Map<String, String> parse(String fileLink) throws IOException {
        Map<String, String> tasks = new HashMap<>();
        BufferedReader reader = getReaderForUrl(fileLink);
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.isEmpty())
                continue;

            String[] parts = line.split(" ", 2);
            String url = parts[0];
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("One or more of the URLs in your bulk file is bad! Please check it and try again. Remember: URLs are expected to have a protocol (http://example.com) attached. " + url);
            }
            String note = parts.length > 1 ? parts[1] : "No note provided."; // check if second part exists
            tasks.put(url, note);
        }
        return tasks;
    }

    @NotNull
    private static BufferedReader getReaderForUrl(String fileLink) throws IOException {
        URL fileUrl;
        try {
            fileUrl = new URL(fileLink);
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("The URL to the bulk file you provided is invalid. Please check it and try again.");
        }
        HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new ConnectException("Sorry, the server for your bulk file returned a bad response code: " + responseCode);
        }
        return new BufferedReader(new InputStreamReader(conn.getInputStream()));
    }
}
