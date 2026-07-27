package com.feedapp.server.storage;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;

@Configuration
public class S3BucketCorsConfig {

    @Bean
    ApplicationRunner applyS3Cors(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.cors-origins}") String corsOrigins
    ) {
        return args -> {
            List<String> origins = List.of(corsOrigins.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            CORSRule rule = CORSRule.builder()
                    .allowedHeaders("*")
                    .allowedMethods("GET", "PUT", "HEAD")
                    .allowedOrigins(origins)
                    .exposeHeaders("ETag", "Content-Type")
                    .maxAgeSeconds(3000)
                    .build();

            s3Client.putBucketCors(PutBucketCorsRequest.builder()
                    .bucket(bucket)
                    .corsConfiguration(CORSConfiguration.builder()
                            .corsRules(rule)
                            .build())
                    .build());
        };
    }
}
