package dev.digitaldragon.util;

import com.github.luben.zstd.ZstdInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InternetArchive {
    /**
     * Downloads a .zst file from the Internet Archive and returns an InputStream for the uncompressed stream.
     *
     * @param url The URL to retrieve the uncompressed stream from.
     * @return The uncompressed stream as an InputStream.
     * @throws IOException If an error occurs while retrieving the stream.
     */
    public static InputStream getUncompressedStream(String url) throws IOException {
        URL zstUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) zstUrl.openConnection();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            return new ZstdInputStream(inputStream);
        } else {
            throw new IOException("Failed to download the file");
        }
    }
}
