package com.feedapp.server.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers
class ImageStorageTest {

    private static final String BUCKET = "feedapp-photos";

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8")
    ).withServices(S3);

    S3Client s3Client;
    S3Presigner s3Presigner;
    ImageStorage imageStorage;
    HttpClient httpClient;

    @BeforeEach
    void setUp() {
        final var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
        );
        final var region = Region.of(localstack.getRegion());
        final var endpoint = localstack.getEndpointOverride(S3);

        s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentials)
                .region(region)
                .forcePathStyle(true)
                .build();

        s3Presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentials)
                .region(region)
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(BUCKET))) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }

        imageStorage = new ImageStorage(s3Client, s3Presigner, BUCKET);
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    @DisplayName("유효한 요청이면 객체 저장 성공")
    void createPresignedUploadUrl() throws Exception {
        final String key = "posts/image.jpg";
        final byte[] bytes = {1, 2, 3};

        final String uploadUrl = imageStorage.createPresignedUploadUrl(key, "image/jpeg");

        assertThat(uploadUrl).isNotBlank();

        final HttpResponse<String> putResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(uploadUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "image/jpeg")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(putResponse.statusCode()).isEqualTo(200);

        final byte[] stored = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build()).asByteArray();
        assertThat(stored).containsExactly(bytes);
    }

    @Test
    @DisplayName("유효한 요청이면 조회 성공")
    void createPresignedDownloadUrl() throws Exception {
        final String key = "posts/download.jpg";
        final byte[] bytes = {1, 2, 3};
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        final String downloadUrl = imageStorage.createPresignedDownloadUrl(key);

        assertThat(downloadUrl).isNotBlank();

        final HttpResponse<byte[]> getResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(getResponse.body()).containsExactly(bytes);
    }

    @Test
    @DisplayName("유효한 요청이면 삭제 성공")
    void delete() {
        final String key = "posts/to-delete.jpg";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(new byte[] {1, 2, 3})
        );

        imageStorage.delete(key);

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build()))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    @DisplayName("없는 키를 삭제해도 예외 없이 종료")
    void deleteWhenMissing() {
        imageStorage.delete("posts/does-not-exist.jpg");
    }
}
