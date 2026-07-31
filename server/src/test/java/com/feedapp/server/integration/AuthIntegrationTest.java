package com.feedapp.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.UUID;

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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

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
    @DisplayName("회원가입, 로그인, 내 정보 조회")
    void signupLoginAndMe() throws Exception {
        final String username = uniqueUsername();
        final String password = "qwer1234";

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(signupJson(username, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username));

        final Cookie[] cookies = loginAndGetCookies(username, password);

        assertThat(findCookie(cookies, "accessToken")).isNotNull();
        assertThat(findCookie(cookies, "accessToken").getValue()).isNotBlank();
        assertThat(findCookie(cookies, "accessToken").getMaxAge()).isEqualTo(1_800);
        assertThat(findCookie(cookies, "refreshToken")).isNotNull();
        assertThat(findCookie(cookies, "refreshToken").getValue()).isNotBlank();
        assertThat(findCookie(cookies, "refreshToken").getMaxAge()).isEqualTo(86_400);

        mockMvc.perform(get("/api/members/me").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @DisplayName("username 중복이면 회원가입 실패")
    void signupWhenUsernameDuplicated() throws Exception {
        final String username = uniqueUsername();
        final String password = "qwer1234";

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(signupJson(username, password)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(signupJson(username, password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("username 중복임"));
    }

    @Test
    @DisplayName("잘못된 비밀번호면 로그인이 실패")
    void loginWithWrongPassword() throws Exception {
        final String username = uniqueUsername();
        final String password = "qwer1234";

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(signupJson(username, password)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/members/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, "wrongpwd")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("뭔가 잘못 입력함"));
    }

    @Test
    @DisplayName("jwt 없이 내 정보 조회 시 실패")
    void meWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 정보임"));
    }

    @Test
    @DisplayName("refresh 후 내 정보 조회")
    void refreshIssuesNewTokens() throws Exception {
        final String username = uniqueUsername();
        final String password = "qwer1234";

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(signupJson(username, password)))
                .andExpect(status().isCreated());

        final Cookie[] loginCookies = loginAndGetCookies(username, password);
        final String oldAccess = findCookie(loginCookies, "accessToken").getValue();
        final Cookie refreshCookie = findCookie(loginCookies, "refreshToken");

        final MvcResult refreshResult = mockMvc.perform(post("/api/members/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andReturn();

        final Cookie[] refreshedCookies = refreshResult.getResponse().getCookies();
        final String newAccess = findCookie(refreshedCookies, "accessToken").getValue();

        assertThat(newAccess).isNotBlank().isNotEqualTo(oldAccess);

        mockMvc.perform(get("/api/members/me").cookie(refreshedCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰 삭제")
    void logoutThenRefreshFails() throws Exception {
        final String username = uniqueUsername();
        final String password = "qwer1234";

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(signupJson(username, password)))
                .andExpect(status().isCreated());

        final Cookie[] loginCookies = loginAndGetCookies(username, password);
        final Cookie refreshCookie = findCookie(loginCookies, "refreshToken");

        mockMvc.perform(post("/api/members/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        mockMvc.perform(post("/api/members/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 정보임"));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 재발급 실패")
    void refreshWithInvalidToken() throws Exception {
        mockMvc.perform(post("/api/members/refresh")
                        .cookie(new Cookie("refreshToken", "not-a-valid-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 정보임"));
    }

    private Cookie[] loginAndGetCookies(String username, String password) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/members/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andReturn();
        return result.getResponse().getCookies();
    }

    private static String uniqueUsername() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String signupJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new com.feedapp.server.member.SignupRequest(username, password));
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new com.feedapp.server.member.LoginRequest(username, password));
    }

    private static Cookie findCookie(Cookie[] cookies, String name) {
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter((cookie) -> name.equals(cookie.getName()))
                .findFirst()
                .orElse(null);
    }
}
