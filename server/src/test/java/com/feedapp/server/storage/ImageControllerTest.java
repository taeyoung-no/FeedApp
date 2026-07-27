package com.feedapp.server.storage;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feedapp.server.auth.JwtAuthFilter;
import com.feedapp.server.auth.JwtTokenProvider;
import com.feedapp.server.auth.SecurityConfig;
import com.feedapp.server.common.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ImageController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ImageService imageService;

    @Test
    @DisplayName("유효한 요청이면 업로드용 presigned URL 발급 성공")
    void createUploadUrl() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        final var request = new CreateUploadUrlRequest("image/jpeg");
        final var response = new PresignedUpload(
                "posts/image.jpg",
                "https://example.com/upload-url"
        );
        when(imageService.createUploadUrl("image/jpeg")).thenReturn(response);

        mockMvc.perform(post("/api/images/upload-url")
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(response.key()))
                .andExpect(jsonPath("$.uploadUrl").value(response.uploadUrl()));

        verify(imageService).createUploadUrl("image/jpeg");
    }

    @Test
    @DisplayName("토큰이 없으면 업로드 URL 발급 실패")
    void createUploadUrlWithoutToken() throws Exception {
        final var request = new CreateUploadUrlRequest("image/jpeg");

        mockMvc.perform(post("/api/images/upload-url")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(imageService, never()).createUploadUrl(anyString());
    }

    @Test
    @DisplayName("허용되지 않는 형식이면 업로드 URL 발급 실패")
    void createUploadUrlWhenUnsupportedType() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        final var request = new CreateUploadUrlRequest("application/pdf");
        when(imageService.createUploadUrl("application/pdf"))
                .thenThrow(new IllegalArgumentException("허용되지 않는 형식"));

        mockMvc.perform(post("/api/images/upload-url")
                        .cookie(new Cookie("accessToken", token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("허용되지 않는 형식"));
    }

    @Test
    @DisplayName("유효한 요청이면 조회용 presigned URL 발급 성공")
    void createDownloadUrl() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        final String key = "posts/image.jpg";
        when(imageService.createDownloadUrl(key)).thenReturn("https://example.com/download-url");

        mockMvc.perform(get("/api/images/download-url")
                        .param("key", key)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com/download-url"));

        verify(imageService).createDownloadUrl(key);
    }

    @Test
    @DisplayName("토큰이 없으면 조회 URL 발급 실패")
    void createDownloadUrlWithoutToken() throws Exception {
        mockMvc.perform(get("/api/images/download-url").param("key", "posts/image.jpg"))
                .andExpect(status().isUnauthorized());

        verify(imageService, never()).createDownloadUrl(anyString());
    }

    @Test
    @DisplayName("키가 없으면 조회 URL 발급 실패")
    void createDownloadUrlWhenKeyMissing() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        when(imageService.createDownloadUrl(" "))
                .thenThrow(new IllegalArgumentException("사진이 없어요"));

        mockMvc.perform(get("/api/images/download-url")
                        .param("key", " ")
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사진이 없어요"));
    }

    @Test
    @DisplayName("유효한 요청이면 이미지 삭제 성공")
    void deleteImage() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        final String key = "posts/image.jpg";

        mockMvc.perform(delete("/api/images")
                        .param("key", key)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isNoContent());

        verify(imageService).delete(key);
    }

    @Test
    @DisplayName("토큰이 없으면 이미지 삭제 실패")
    void deleteImageWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/images").param("key", "posts/image.jpg"))
                .andExpect(status().isUnauthorized());

        verify(imageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("키가 없으면 삭제 실패")
    void deleteImageWhenKeyMissing() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        doThrow(new IllegalArgumentException("삭제할 사진이 없어요"))
                .when(imageService).delete(eq(" "));

        mockMvc.perform(delete("/api/images")
                        .param("key", " ")
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("삭제할 사진이 없어요"));
    }
}
