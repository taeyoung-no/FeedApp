package com.feedapp.server.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import java.io.ByteArrayInputStream;

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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Testcontainers
class ImageStorageTest {

    private static final String BUCKET = "feedapp-photos";

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8")
    ).withServices(S3);

    S3Client s3Client;
    ImageStorage imageStorage;

    @BeforeEach
    void setUp() {
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .region(Region.of(localstack.getRegion()))
                .forcePathStyle(true)
                .build();

        if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(BUCKET))) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }

        imageStorage = new ImageStorage(s3Client, BUCKET, localstack.getRegion());
    }

    @Test
    @DisplayName("유효한 요청이면 객체를 저장하고 key와 url을 반환")
    void upload() {
        final String key = "posts/image.jpg";
        final byte[] bytes = {1, 2, 3};
        final var content = new ByteArrayInputStream(bytes);

        final StoredImage result = imageStorage.upload(key, content, "image/jpeg", bytes.length);

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.url()).isEqualTo(
                "https://%s.s3.%s.amazonaws.com/%s".formatted(BUCKET, localstack.getRegion(), key)
        );

        byte[] stored = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build()).asByteArray();
        assertThat(stored).containsExactly(bytes);

        String contentType = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build()).contentType();
        assertThat(contentType).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("유효한 요청이면 삭제 성공")
    void delete() {
        final String key = "posts/to-delete.jpg";
        final byte[] bytes = {9, 8, 7};
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        imageStorage.delete(key);

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build()))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    @DisplayName("유효하지 않은 키면 예외 없이 종료")
    void deleteWhenMissing() {
        imageStorage.delete("posts/does-not-exist.jpg");
    }
}
