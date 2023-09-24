package dev.digitaldragon.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class TransferUploader {
    /**
     * Uploads a file to transfer.sh.
     *
     * @param file The file to be uploaded.
     * @param name The name of the file on the destination server.
     * @return The response from the server.
     * @throws IOException If there was an error during the upload process.
     */
    public static String uploadFileToTransferSh(File file, String name) throws IOException {
        if (name.contains("/")) {
            throw new IllegalArgumentException("The name of the file cannot contain a slash (/)!");
        }
        String transferShUrl = "https://transfer.archivete.am/" + name;

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

    /**
     * Uploads a file to the transfer.sh provider.
     *
     * @param file The file to be uploaded.
     * @return The URL of the uploaded file on TransferSh.
     * @throws IOException if an I/O error occurs while uploading the file.
     */
    public static String uploadFileToTransferSh(File file) throws IOException {
        return uploadFileToTransferSh(file, file.getName());
    }
}
