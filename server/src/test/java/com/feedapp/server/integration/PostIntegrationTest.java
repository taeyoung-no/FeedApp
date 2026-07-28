package com.feedapp.server.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.feedapp.server.member.LoginRequest;
import com.feedapp.server.member.SignupRequest;
import com.feedapp.server.post.CreatePostRequest;
import com.feedapp.server.post.UpdatePostRequest;
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
class PostIntegrationTest {

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
        registry.add("jwt.expiration-ms", () -> "3600000");

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
    @DisplayName("게시글 작성, 상세 조회")
    void createAndGetPost() throws Exception {
        final AuthSession session = signupAndLogin();

        final MvcResult created = mockMvc.perform(post("/api/posts")
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(postJson("title", "content")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.author").value(session.username()))
                .andExpect(jsonPath("$.images.length()").value(0))
                .andReturn();

        final long postId = readId(created);

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.author").value(session.username()));
    }

    @Test
    @DisplayName("게시글 목록 조회")
    void findAllPosts() throws Exception {
        final AuthSession session = signupAndLogin();
        final String title1 = "t1-" + uniqueUsername();
        final String title2 = "t2-" + uniqueUsername();

        mockMvc.perform(post("/api/posts")
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(postJson(title1, "content1")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts")
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(postJson(title2, "content2")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].title", hasItems(title1, title2)));
    }

    @Test
    @DisplayName("jwt 없이 게시글 작성 시 실패")
    void createWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(APPLICATION_JSON)
                        .content(postJson("title", "content")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("없는 게시글 조회 시 실패")
    void getWhenNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("게시글 수정")
    void updatePost() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies(), "oldTitle", "oldContent");

        mockMvc.perform(put("/api/posts/{id}", postId)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("newTitle", "newContent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value("newTitle"))
                .andExpect(jsonPath("$.content").value("newContent"));

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("newTitle"))
                .andExpect(jsonPath("$.content").value("newContent"));
    }

    @Test
    @DisplayName("작성자가 아니면 게시글 수정 실패")
    void updateWhenNotAuthor() throws Exception {
        final AuthSession owner = signupAndLogin();
        final long postId = createPost(owner.cookies(), "title", "content");

        final AuthSession other = signupAndLogin();

        mockMvc.perform(put("/api/posts/{id}", postId)
                        .cookie(other.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("hacked", "hacked")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 없음"));
    }

    @Test
    @DisplayName("없는 게시글 수정 시 실패")
    void updateWhenNotFound() throws Exception {
        final AuthSession session = signupAndLogin();

        mockMvc.perform(put("/api/posts/{id}", 99999L)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("title", "content")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("jwt 없이 게시글 수정 시 실패")
    void updateWithoutAuth() throws Exception {
        mockMvc.perform(put("/api/posts/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("title", "content")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글 삭제")
    void deletePost() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies(), "title", "content");

        mockMvc.perform(delete("/api/posts/{id}", postId).cookie(session.cookies()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("작성자가 아니면 게시글 삭제 실패")
    void deleteWhenNotAuthor() throws Exception {
        final AuthSession owner = signupAndLogin();
        final long postId = createPost(owner.cookies(), "title", "content");

        final AuthSession other = signupAndLogin();

        mockMvc.perform(delete("/api/posts/{id}", postId).cookie(other.cookies()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 없음"));
    }

    @Test
    @DisplayName("없는 게시글 삭제 시 실패")
    void deleteWhenNotFound() throws Exception {
        final AuthSession session = signupAndLogin();

        mockMvc.perform(delete("/api/posts/{id}", 99999L).cookie(session.cookies()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("jwt 없이 게시글 삭제 시 실패")
    void deleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", 1L))
                .andExpect(status().isUnauthorized());
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

    private long createPost(Cookie[] cookies, String title, String content) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/posts")
                        .cookie(cookies)
                        .contentType(APPLICATION_JSON)
                        .content(postJson(title, content)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long readId(MvcResult result) throws Exception {
        final JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String postJson(String title, String content) throws Exception {
        return objectMapper.writeValueAsString(new CreatePostRequest(title, content, List.of()));
    }

    private String updateJson(String title, String content) throws Exception {
        return objectMapper.writeValueAsString(new UpdatePostRequest(title, content, List.of()));
    }

    private static String uniqueUsername() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record AuthSession(String username, Cookie[] cookies) {
    }
}
