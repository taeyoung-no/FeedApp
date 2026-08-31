package com.feedapp.server.like;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feedapp.server.auth.JwtAuthFilter;
import com.feedapp.server.auth.JwtTokenProvider;
import com.feedapp.server.auth.SecurityConfig;
import com.feedapp.server.common.GlobalExceptionHandler;
import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.member.ConflictException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostLikeController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class PostLikeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    PostLikeService postLikeService;

    @Test
    @DisplayName("유효한 요청이면 좋아요 성공")
    void like() throws Exception {
        final Long postId = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);

        mockMvc.perform(post("/api/posts/{postId}/likes", postId)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isCreated());

        verify(postLikeService).like(postId, username);
    }

    @Test
    @DisplayName("토큰이 없으면 좋아요 실패")
    void likeWithoutToken() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/likes", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글이 없으면 좋아요 실패")
    void likeWhenPostNotFound() throws Exception {
        final Long postId = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);
        doThrow(new NotFoundException("게시글 없음"))
                .when(postLikeService).like(postId, username);

        mockMvc.perform(post("/api/posts/{postId}/likes", postId)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("이미 좋아요 했으면 실패")
    void likeWhenAlreadyLiked() throws Exception {
        final Long postId = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);
        doThrow(new ConflictException("이미 좋아요 함"))
                .when(postLikeService).like(postId, username);

        mockMvc.perform(post("/api/posts/{postId}/likes", postId)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 좋아요 함"));
    }

    @Test
    @DisplayName("유효한 요청이면 좋아요 취소 성공")
    void unlike() throws Exception {
        final Long postId = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);

        mockMvc.perform(delete("/api/posts/{postId}/likes", postId)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNoContent());

        verify(postLikeService).unlike(postId, username);
    }

    @Test
    @DisplayName("토큰이 없으면 좋아요 취소 실패")
    void unlikeWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}/likes", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("좋아요가 없으면 취소 실패")
    void unlikeWhenNotFound() throws Exception {
        final Long postId = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);
        doThrow(new NotFoundException("좋아요 안 했음"))
                .when(postLikeService).unlike(postId, username);

        mockMvc.perform(delete("/api/posts/{postId}/likes", postId)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("좋아요 안 했음"));
    }
}
