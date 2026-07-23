package com.feedapp.server.comment;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.feedapp.server.auth.JwtAuthFilter;
import com.feedapp.server.auth.JwtTokenProvider;
import com.feedapp.server.auth.SecurityConfig;
import com.feedapp.server.common.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

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
}
