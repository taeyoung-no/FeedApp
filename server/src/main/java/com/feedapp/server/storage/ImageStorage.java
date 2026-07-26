package com.feedapp.server.storage;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class ImageStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public ImageStorage(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    public StoredImage upload(String key, InputStream content, String contentType, long contentLength) {
        var request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));

        String url = "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
        return new StoredImage(key, url);
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
