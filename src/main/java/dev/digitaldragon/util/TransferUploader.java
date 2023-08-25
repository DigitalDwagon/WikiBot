package dev.digitaldragon.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class TransferUploader {
    public static String uploadFileToTransferSh(File file) throws IOException {
        String transferShUrl = "https://transfer.archivete.am/";

        HttpURLConnection connection = null;
        try {
            // Open a connection to transfer.sh
            URL url = new URL(transferShUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");

            // Open the file and read its contents
            FileInputStream fileInputStream = new FileInputStream(file);
            OutputStream outputStream = connection.getOutputStream();
            Files.copy(file.toPath(), outputStream);

            // Get the response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = in.readLine();
                in.close();
                return response;
            } else {
                throw new IOException("Upload failed with response code: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
