package com.feedapp.server.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.feedapp.server.member.LoginRequest;
import com.feedapp.server.member.SignupRequest;
import com.feedapp.server.post.CreatePostRequest;
import com.redis.testcontainers.RedisContainer;
import jakarta.servlet.http.Cookie;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LikeIntegrationTest {

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

    @Test
    @DisplayName("좋아요, 상세 조회, 취소")
    void likeGetUnlike() throws Exception {
        final AuthSession owner = signupAndLogin();
        final long postId = createPost(owner.cookies());
        final AuthSession liker = signupAndLogin();

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));

        mockMvc.perform(post("/api/posts/{postId}/likes", postId).cookie(liker.cookies()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts/{id}", postId).cookie(liker.cookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));

        mockMvc.perform(get("/api/posts/{id}", postId).cookie(owner.cookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(1));

        mockMvc.perform(delete("/api/posts/{postId}/likes", postId).cookie(liker.cookies()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{id}", postId).cookie(liker.cookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    @DisplayName("같은 글에 두 번 좋아요하면 실패")
    void likeTwice() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());

        mockMvc.perform(post("/api/posts/{postId}/likes", postId).cookie(session.cookies()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts/{postId}/likes", postId).cookie(session.cookies()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 좋아요 함"));
    }

    @Test
    @DisplayName("jwt 없이 좋아요 시 실패")
    void likeWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/likes", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("없는 게시글 좋아요 시 실패")
    void likeWhenPostNotFound() throws Exception {
        final AuthSession session = signupAndLogin();

        mockMvc.perform(post("/api/posts/{postId}/likes", 99999L).cookie(session.cookies()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("좋아요 없이 취소 시 실패")
    void unlikeWhenNotLiked() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());

        mockMvc.perform(delete("/api/posts/{postId}/likes", postId).cookie(session.cookies()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("좋아요 없음"));
    }

    private AuthSession signupAndLogin() throws Exception {
        final String username = uniqueUsername();
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

    private long createPost(Cookie[] cookies) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/posts")
                        .cookie(cookies)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("title", "content", List.of()))))
                .andExpect(status().isCreated())
                .andReturn();
        final JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private static String uniqueUsername() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record AuthSession(String username, Cookie[] cookies) {
    }
}
