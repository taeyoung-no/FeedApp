package com.feedapp.server.post;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
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
                new PostResponse(1L, "title1", "content1", "author1", createdAt1, List.of()),
                new PostResponse(2L, "title2", "content2", "author2", createdAt2, List.of())
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
    @DisplayName("게시글이 있으면 id로 상세 조회")
    void findById() throws Exception {
        final Long id = 1L;
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postService.findById(id)).thenReturn(
                new PostResponse(id, "title", "content", "author", createdAt, List.of())
        );

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.author").value("author"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00"));
    }

    @Test
    @DisplayName("게시글이 없으면 상세 조회 실패")
    void findByIdWhenNotFound() throws Exception {
        final Long id = 1L;
        when(postService.findById(id)).thenThrow(new NotFoundException("게시글 없음"));

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("유효한 요청이면 토큰 username으로 게시글 작성 성공")
    void create() throws Exception {
        final String username = "author";
        final var request = new CreatePostRequest("title", "content", null);
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final String token = jwtTokenProvider.createAccessToken(username);
        when(postService.create(request.title(), request.content(), username, null))
                .thenReturn(new PostResponse(1L, request.title(), request.content(), username, createdAt, List.of()));

        mockMvc.perform(post("/api/posts")
                        .cookie(new Cookie("accessToken", token))
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
        final var request = new CreatePostRequest("title", "content", null);

        mockMvc.perform(post("/api/posts")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리프레시 토큰이면 게시글 작성 실패")
    void createWithRefreshToken() throws Exception {
        final var request = new CreatePostRequest("title", "content", null);
        final String token = jwtTokenProvider.createRefreshToken("author");

        mockMvc.perform(post("/api/posts")
                        .cookie(new Cookie("refreshToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 요청이면 게시글 삭제 성공")
    void deletePost() throws Exception {
        final Long id = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);

        mockMvc.perform(delete("/api/posts/{id}", id)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNoContent());

        verify(postService).delete(id, username);
    }

    @Test
    @DisplayName("토큰이 없으면 게시글 삭제 실패")
    void deletePostWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글이 없으면 삭제 실패")
    void deletePostWhenNotFound() throws Exception {
        final Long id = 1L;
        final String username = "author";
        final String token = jwtTokenProvider.createAccessToken(username);
        doThrow(new NotFoundException("게시글 없음"))
                .when(postService).delete(id, username);

        mockMvc.perform(delete("/api/posts/{id}", id)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("작성자가 아니면 삭제 실패")
    void deletePostWhenNotAuthor() throws Exception {
        final Long id = 1L;
        final String username = "other";
        final String token = jwtTokenProvider.createAccessToken(username);
        doThrow(new ForbiddenException("권한 없음"))
                .when(postService).delete(id, username);

        mockMvc.perform(delete("/api/posts/{id}", id)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 없음"));
    }

    @Test
    @DisplayName("유효한 요청이면 게시글 수정 성공")
    void updatePost() throws Exception {
        final Long id = 1L;
        final String username = "author";
        final var request = new UpdatePostRequest("newTitle", "newContent", List.of());
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final String token = jwtTokenProvider.createAccessToken(username);
        when(postService.update(id, request.title(), request.content(), username, request.imageKeys()))
                .thenReturn(new PostResponse(id, request.title(), request.content(), username, createdAt, List.of()));

        mockMvc.perform(put("/api/posts/{id}", id)
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value(request.title()))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.author").value(username))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00"));
    }

    @Test
    @DisplayName("토큰이 없으면 게시글 수정 실패")
    void updatePostWithoutToken() throws Exception {
        final var request = new UpdatePostRequest("newTitle", "newContent", List.of());

        mockMvc.perform(put("/api/posts/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글이 없으면 수정 실패")
    void updatePostWhenNotFound() throws Exception {
        final Long id = 1L;
        final String username = "author";
        final var request = new UpdatePostRequest("newTitle", "newContent", List.of());
        final String token = jwtTokenProvider.createAccessToken(username);
        when(postService.update(id, request.title(), request.content(), username, request.imageKeys()))
                .thenThrow(new NotFoundException("게시글 없음"));

        mockMvc.perform(put("/api/posts/{id}", id)
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글 없음"));
    }

    @Test
    @DisplayName("작성자가 아니면 수정 실패")
    void updatePostWhenNotAuthor() throws Exception {
        final Long id = 1L;
        final String username = "other";
        final var request = new UpdatePostRequest("newTitle", "newContent", List.of());
        final String token = jwtTokenProvider.createAccessToken(username);
        when(postService.update(id, request.title(), request.content(), username, request.imageKeys()))
                .thenThrow(new ForbiddenException("권한 없음"));

        mockMvc.perform(put("/api/posts/{id}", id)
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 없음"));
    }

    @Test
    @DisplayName("이미지 포함 유효한 요청이면 게시글 수정 성공")
    void updatePostWithImageKeys() throws Exception {
        final Long id = 1L;
        final String username = "author";
        final var imageKeys = List.of("posts/a.jpg", "posts/b.png");
        final var images = List.of(
                new PostImageResponse("posts/a.jpg", "https://example.com/a"),
                new PostImageResponse("posts/b.png", "https://example.com/b")
        );
        final var request = new UpdatePostRequest("newTitle", "newContent", imageKeys);
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final String token = jwtTokenProvider.createAccessToken(username);
        when(postService.update(id, request.title(), request.content(), username, imageKeys))
                .thenReturn(new PostResponse(id, request.title(), request.content(), username, createdAt, images));

        mockMvc.perform(put("/api/posts/{id}", id)
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].key").value("posts/a.jpg"))
                .andExpect(jsonPath("$.images[0].url").value("https://example.com/a"))
                .andExpect(jsonPath("$.images[1].key").value("posts/b.png"))
                .andExpect(jsonPath("$.images[1].url").value("https://example.com/b"));
    }

    @Test
    @DisplayName("이미지 포함 유효한 요청이면 게시글 작성 성공")
    void createWithImageKeys() throws Exception {
        final String username = "author";
        final var imageKeys = List.of("posts/a.jpg", "posts/b.png");
        final var images = List.of(
                new PostImageResponse("posts/a.jpg", "https://example.com/a"),
                new PostImageResponse("posts/b.png", "https://example.com/b")
        );
        final var request = new CreatePostRequest("title", "content", imageKeys);
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final String token = jwtTokenProvider.createAccessToken(username);
        when(postService.create(request.title(), request.content(), username, imageKeys))
                .thenReturn(new PostResponse(
                        1L, request.title(), request.content(), username, createdAt, images
                ));

        mockMvc.perform(post("/api/posts")
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value(request.title()))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.author").value(username))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].key").value("posts/a.jpg"))
                .andExpect(jsonPath("$.images[0].url").value("https://example.com/a"))
                .andExpect(jsonPath("$.images[1].key").value("posts/b.png"))
                .andExpect(jsonPath("$.images[1].url").value("https://example.com/b"));
    }

    @Test
    @DisplayName("상세 조회 시 이미지 key와 url 포함")
    void findByIdWithImages() throws Exception {
        final Long id = 1L;
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var images = List.of(new PostImageResponse("posts/a.jpg", "https://example.com/a"));
        when(postService.findById(id)).thenReturn(
                new PostResponse(id, "title", "content", "author", createdAt, images)
        );

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].key").value("posts/a.jpg"))
                .andExpect(jsonPath("$.images[0].url").value("https://example.com/a"));
    }

    @Test
    @DisplayName("상세 조회 시 이미지가 없으면 빈 목록 반환")
    void findByIdWithoutImages() throws Exception {
        final Long id = 1L;
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postService.findById(id)).thenReturn(
                new PostResponse(id, "title", "content", "author", createdAt, List.of())
        );

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(0));
    }
}

