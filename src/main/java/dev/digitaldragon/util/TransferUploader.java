package dev.digitaldragon.util;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdOutputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TransferUploader {
    public static String compressAndUpload(String destinationUrl, File inputFile) throws IOException {
        if (!inputFile.exists()) {
            throw new FileNotFoundException("File not found: " + inputFile.getAbsolutePath());
        }

        System.out.println("Dest " + destinationUrl);
        URL url = new URL(destinationUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setChunkedStreamingMode(0); // Enables streaming mode

        // Stream the file through the Zstd compressor into the HTTP request
        try (
                OutputStream httpOutputStream = connection.getOutputStream();
                ZstdOutputStream zstdOutputStream = new ZstdOutputStream(httpOutputStream).setLevel(Zstd.maxCompressionLevel());
                FileInputStream fileInputStream = new FileInputStream(inputFile)
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                zstdOutputStream.write(buffer, 0, bytesRead);
            }
        }

        // Read and return the response body from the server
        InputStream responseStream;
        if (connection.getResponseCode() >= 400) {
            responseStream = connection.getErrorStream();
        } else {
            responseStream = connection.getInputStream();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\n");
            }
            return responseBuilder.toString().trim();
        }
    }

}
