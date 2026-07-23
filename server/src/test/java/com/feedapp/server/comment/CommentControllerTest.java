package com.feedapp.server.comment;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.feedapp.server.auth.JwtAuthFilter;
import com.feedapp.server.auth.JwtTokenProvider;
import com.feedapp.server.auth.SecurityConfig;
import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.GlobalExceptionHandler;
import com.feedapp.server.common.NotFoundException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    CommentService commentService;

    @Test
    @DisplayName("댓글이 있으면 생성 시각 내림차순 목록을 반환")
    void findByPostId() throws Exception {
        final Long postId = 1L;
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(commentService.findByPostId(postId)).thenReturn(List.of(
                new CommentResponse(1L, postId, "content1", "author1", createdAt1),
                new CommentResponse(2L, postId, "content2", "author2", createdAt2)
        ));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].postId").value(postId))
                .andExpect(jsonPath("$[0].content").value("content1"))
                .andExpect(jsonPath("$[0].author").value("author1"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-02T10:00:00"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].postId").value(postId))
                .andExpect(jsonPath("$[1].content").value("content2"))
                .andExpect(jsonPath("$[1].author").value("author2"))
                .andExpect(jsonPath("$[1].createdAt").value("2026-01-01T10:00:00"));
    }

    @Test
    @DisplayName("댓글이 없으면 빈 목록을 반환")
    void findByPostIdWhenEmpty() throws Exception {
        final Long postId = 1L;
        when(commentService.findByPostId(postId)).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("유효한 요청이면 토큰 username으로 댓글 작성 성공")
    void create() throws Exception {
        final Long postId = 1L;
        final String username = "author";
        final var request = new CreateCommentRequest("content");
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final String token = jwtTokenProvider.createAccessToken(username);
        when(commentService.create(postId, request.content(), username))
                .thenReturn(new CommentResponse(1L, postId, request.content(), username, createdAt));

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.author").value(username))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00"));
    }

    @Test
    @DisplayName("토큰이 없으면 댓글 작성 실패")
    void createWithoutToken() throws Exception {
        final var request = new CreateCommentRequest("content");

        mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글이 없으면 댓글 작성 실패")
    void createWhenPostNotFound() throws Exception {
        final Long postId = 1L;
        final String username = "author";
        final var request = new CreateCommentRequest("content");
        final String token = jwtTokenProvider.createAccessToken(username);
        when(commentService.create(postId, request.content(), username))
                .thenThrow(new NotFoundException("게시글 없음"));

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("리프레시 토큰이면 댓글 작성 실패")
    void createWithRefreshToken() throws Exception {
        final var request = new CreateCommentRequest("content");
        final String token = jwtTokenProvider.createRefreshToken("author");

        mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                        .cookie(new Cookie("refreshToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 요청이면 댓글 삭제 성공")
    void deleteComment() throws Exception {
        final Long id = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);

        mockMvc.perform(delete("/api/comments/{id}", id)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNoContent());

        verify(commentService).delete(id, username);
    }

    @Test
    @DisplayName("토큰이 없으면 댓글 삭제 실패")
    void deleteCommentWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/comments/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("댓글이 없으면 삭제 실패")
    void deleteCommentWhenNotFound() throws Exception {
        final Long id = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);
        doThrow(new NotFoundException("댓글 없음"))
                .when(commentService).delete(id, username);

        mockMvc.perform(delete("/api/comments/{id}", id)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글 없음"));
    }

    @Test
    @DisplayName("작성자가 아니면 삭제 실패")
    void deleteCommentWhenNotAuthor() throws Exception {
        final Long id = 1L;
        final String username = "other";
        final String token = jwtTokenProvider.createAccessToken(username);
        doThrow(new ForbiddenException("권한 없음"))
                .when(commentService).delete(id, username);

        mockMvc.perform(delete("/api/comments/{id}", id)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 없음"));
    }
}
