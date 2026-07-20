package com.feedapp.server;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class})
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    PostService postService;

    @Test
    @DisplayName("게시글이 있으면 생성 시각 내림차순 목록을 반환")
    void findAll() throws Exception {
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postService.findAll()).thenReturn(List.of(
                new PostResponse(1L, "title1", "content1", "author1", createdAt1),
                new PostResponse(2L, "title2", "content2", "author2", createdAt2)
        ));

        mockMvc.perform(get("/api/posts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("title1"))
                .andExpect(jsonPath("$[0].content").value("content1"))
                .andExpect(jsonPath("$[0].author").value("author1"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-02T10:00:00"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("title2"))
                .andExpect(jsonPath("$[1].content").value("content2"))
                .andExpect(jsonPath("$[1].author").value("author2"))
                .andExpect(jsonPath("$[1].createdAt").value("2026-01-01T10:00:00"));
    }

    @Test
    @DisplayName("게시글이 없으면 빈 목록을 반환")
    void findAllWhenEmpty() throws Exception {
        when(postService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("유효한 요청이면 토큰 username으로 게시글 작성 성공")
    void create() throws Exception {
        final String username = "author";
        final var request = new CreatePostRequest("title", "content");
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final String token = jwtTokenProvider.createToken(username);
        when(postService.create(request.title(), request.content(), username))
                .thenReturn(new PostResponse(1L, request.title(), request.content(), username, createdAt));

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value(request.title()))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.author").value(username))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00"));
    }

    @Test
    @DisplayName("토큰이 없으면 게시글 작성 실패")
    void createWithoutToken() throws Exception {
        final var request = new CreatePostRequest("title", "content");

        mockMvc.perform(post("/api/posts")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

