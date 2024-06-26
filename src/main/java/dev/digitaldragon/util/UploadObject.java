package dev.digitaldragon.util;

import com.google.cloud.storage.*;

import java.io.IOException;
import java.nio.file.Paths;

public class UploadObject {
    /**
     * Uploads a file to a Google Cloud Storage bucket.
     *
     * @param projectId The ID of the Google Cloud project.
     * @param bucketName The name of the Google Cloud Storage bucket.
     * @param objectName The name of the object to be created or overwritten.
     * @param filePath The path of the file to be uploaded.
     * @param contentType The content type of the file, for example text/plain or image/png
     * @param contentDisposition The content disposition of the file.
     *
     * @throws IOException If an I/O error occurs while reading the file or uploading to the bucket.
     */
    public static void uploadObject(
            String projectId, String bucketName, String objectName, String filePath, String contentType, String contentDisposition) throws IOException {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The ID of your GCS object
        // String objectName = "your-object-name";

        // The path to your file to upload
        // String filePath = "path/to/your/file"

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // Optional: set a generation-match precondition to avoid potential race
        // conditions and data corruptions. The request returns a 412 error if the
        // preconditions are not met.
        Storage.BlobWriteOption precondition;
        /*if (storage.get(bucketName, objectName) == null) {
            // For a target object that does not yet exist, set the DoesNotExist precondition.
            // This will cause the request to fail if the object is created before the request runs.
            precondition = Storage.BlobWriteOption.doesNotExist();
        } else {
            // If the destination already exists in your bucket, instead set a generation-match
            // precondition. This will cause the request to fail if the existing object's generation
            // changes before the request runs.
            precondition =
                    Storage.BlobWriteOption.generationMatch();
        }*/
        Blob uploaded = storage.createFrom(blobInfo, Paths.get(filePath));
        uploaded.toBuilder().setContentType(contentType).setContentDisposition(contentDisposition).build().update();

        System.out.println(
                "File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
    }
}
