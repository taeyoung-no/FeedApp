package com.feedapp.server.integration;

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

import com.feedapp.server.comment.CreateCommentRequest;
import com.feedapp.server.comment.UpdateCommentRequest;
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
class CommentIntegrationTest {

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
    @DisplayName("댓글 작성")
    void createAndListComments() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());

        final MvcResult created = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(commentJson("Hello, World!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.content").value("Hello, World!"))
                .andExpect(jsonPath("$.author").value(session.username()))
                .andReturn();

        final long commentId = readId(created);

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].postId").value(postId))
                .andExpect(jsonPath("$[0].content").value("Hello, World!"))
                .andExpect(jsonPath("$[0].author").value(session.username()));
    }

    @Test
    @DisplayName("댓글 조회")
    void listMultipleComments() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());
        final String c1 = "c1-" + uniqueUsername();
        final String c2 = "c2-" + uniqueUsername();

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(commentJson(c1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(commentJson(c2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].content", hasItems(c1, c2)));
    }

    @Test
    @DisplayName("댓글 없는 게시글 댓글 조회")
    void listWhenEmpty() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("jwt 없이 댓글 작성 시 실패")
    void createWithoutAuth() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(APPLICATION_JSON)
                        .content(commentJson("hello")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("없는 게시글에 댓글 작성 시 실패")
    void createWhenPostNotFound() throws Exception {
        final AuthSession session = signupAndLogin();

        mockMvc.perform(post("/api/posts/{postId}/comments", 99999L)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(commentJson("hello")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("댓글 수정")
    void updateComment() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());
        final long commentId = createComment(session.cookies(), postId, "old");

        mockMvc.perform(put("/api/comments/{id}", commentId)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("new")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.content").value("new"))
                .andExpect(jsonPath("$.author").value(session.username()));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("new"));
    }

    @Test
    @DisplayName("작성자가 아니면 댓글 수정 실패")
    void updateWhenNotAuthor() throws Exception {
        final AuthSession owner = signupAndLogin();
        final long postId = createPost(owner.cookies());
        final long commentId = createComment(owner.cookies(), postId, "mine");

        final AuthSession other = signupAndLogin();

        mockMvc.perform(put("/api/comments/{id}", commentId)
                        .cookie(other.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("hacked")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 없음"));
    }

    @Test
    @DisplayName("없는 댓글 수정 시 실패")
    void updateWhenNotFound() throws Exception {
        final AuthSession session = signupAndLogin();

        mockMvc.perform(put("/api/comments/{id}", 99999L)
                        .cookie(session.cookies())
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("new")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글 없음"));
    }

    @Test
    @DisplayName("jwt 없이 댓글 수정 시 실패")
    void updateWithoutAuth() throws Exception {
        mockMvc.perform(put("/api/comments/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(updateJson("new")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("댓글 삭제")
    void deleteComment() throws Exception {
        final AuthSession session = signupAndLogin();
        final long postId = createPost(session.cookies());
        final long commentId = createComment(session.cookies(), postId, "bye");

        mockMvc.perform(delete("/api/comments/{id}", commentId).cookie(session.cookies()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("작성자가 아니면 댓글 삭제 실패")
    void deleteWhenNotAuthor() throws Exception {
        final AuthSession owner = signupAndLogin();
        final long postId = createPost(owner.cookies());
        final long commentId = createComment(owner.cookies(), postId, "mine");

        final AuthSession other = signupAndLogin();

        mockMvc.perform(delete("/api/comments/{id}", commentId).cookie(other.cookies()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 없음"));
    }

    @Test
    @DisplayName("없는 댓글 삭제 시 실패")
    void deleteWhenNotFound() throws Exception {
        final AuthSession session = signupAndLogin();

        mockMvc.perform(delete("/api/comments/{id}", 99999L).cookie(session.cookies()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글 없음"));
    }

    @Test
    @DisplayName("jwt 없이 댓글 삭제 시 실패")
    void deleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/comments/{id}", 1L))
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

    private long createPost(Cookie[] cookies) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/posts")
                        .cookie(cookies)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePostRequest("title", "content", List.of()))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long createComment(Cookie[] cookies, long postId, String content) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .cookie(cookies)
                        .contentType(APPLICATION_JSON)
                        .content(commentJson(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long readId(MvcResult result) throws Exception {
        final JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String commentJson(String content) throws Exception {
        return objectMapper.writeValueAsString(new CreateCommentRequest(content));
    }

    private String updateJson(String content) throws Exception {
        return objectMapper.writeValueAsString(new UpdateCommentRequest(content));
    }

    private static String uniqueUsername() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record AuthSession(String username, Cookie[] cookies) {
    }
}
