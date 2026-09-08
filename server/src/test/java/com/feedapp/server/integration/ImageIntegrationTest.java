package com.feedapp.server.integration;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;

import com.feedapp.server.member.LoginRequest;
import com.feedapp.server.member.SignupRequest;
import com.feedapp.server.storage.CreateUploadUrlRequest;
import com.redis.testcontainers.RedisContainer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ImageIntegrationTest {

    @Container
    static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("feedapp")
            .withUsername("admin")
            .withPassword("qwer1234");

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("jwt.secret", () -> "jwt-secret-1234567890-very-long-string");
        registry.add("jwt.access-expiration-ms", () -> "1800000");
        registry.add("jwt.refresh-expiration-ms", () -> "86400000");

        registry.add("aws.s3.bucket", () -> "feedapp-photos");
        registry.add("aws.s3.region", () -> "ap-northeast-2");
        registry.add("aws.s3.cors-origins", () -> "http://localhost:5173");
    }

    @MockitoBean
    S3Client s3Client;

    @MockitoBean
    S3Presigner s3Presigner;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void stubS3() throws Exception {
        final PresignedPutObjectRequest put = mock(PresignedPutObjectRequest.class);
        when(put.url()).thenReturn(URI.create("https://s3.example.com/upload").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(put);

        final PresignedGetObjectRequest get = mock(PresignedGetObjectRequest.class);
        when(get.url()).thenReturn(URI.create("https://s3.example.com/download").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(get);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());
    }

    @Test
    @DisplayName("이미지 업로드 URL 발급, 조회 URL 발급, 삭제")
    void createUploadUrlThenDownloadUrlThenDelete() throws Exception {
        final AuthSession session = signupAndLogin();

        final MvcResult uploaded = mockMvc.perform(post("/api/images/upload-url")
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUploadUrlRequest("image/jpeg"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key", startsWith("posts/")))
                .andExpect(jsonPath("$.key", endsWith(".jpg")))
                .andExpect(jsonPath("$.uploadUrl").value("https://s3.example.com/upload"))
                .andReturn();

        final String key = objectMapper.readTree(uploaded.getResponse().getContentAsString()).get("key").asString();

        mockMvc.perform(get("/api/images/download-url")
                        .param("key", key)
                        .cookie(session.cookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://s3.example.com/download"));

        mockMvc.perform(delete("/api/images")
                        .param("key", key)
                        .cookie(session.cookies()))
                .andExpect(status().isNoContent());

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("jwt 없이 이미지 업로드 URL 발급 시 실패")
    void createUploadUrlWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/images/upload-url")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUploadUrlRequest("image/jpeg"))))
                .andExpect(status().isUnauthorized());
    }

    private AuthSession signupAndLogin() throws Exception {
        final String username = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        final String password = "qwer1234";

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(username, password))))
                .andExpect(status().isCreated());

        final MvcResult result = mockMvc.perform(post("/api/members/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        return new AuthSession(username, result.getResponse().getCookies());
    }

    private record AuthSession(String username, Cookie[] cookies) {
    }
}
