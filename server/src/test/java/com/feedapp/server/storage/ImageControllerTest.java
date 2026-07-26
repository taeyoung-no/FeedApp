package com.feedapp.server.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ImageService imageService;

    @Test
    @DisplayName("유효한 요청이면 이미지 업로드 성공")
    void upload() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        final byte[] bytes = {1, 2, 3};
        final var file = new MockMultipartFile("file", "image.jpg", "image/jpeg", bytes);
        final var stored = new StoredImage(
                "posts/image.jpg",
                "https://feedapp-photos.s3.ap-northeast-2.amazonaws.com/posts/image.jpg"
        );
        when(imageService.upload(any(InputStream.class), eq("image/jpeg"), eq(3L)))
                .thenReturn(stored);

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(stored.key()))
                .andExpect(jsonPath("$.url").value(stored.url()));

        verify(imageService).upload(any(InputStream.class), eq("image/jpeg"), eq(3L));
    }

    @Test
    @DisplayName("토큰이 없으면 이미지 업로드 실패")
    void uploadWithoutToken() throws Exception {
        final var file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/images").file(file))
                .andExpect(status().isUnauthorized());

        verify(imageService, never()).upload(any(), any(), anyLong());
    }

    @Test
    @DisplayName("허용되지 않는 형식이면 업로드 실패")
    void uploadWhenUnsupportedType() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("author");
        final var file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[] {1}
        );
        when(imageService.upload(any(InputStream.class), eq("application/pdf"), eq(1L)))
                .thenThrow(new IllegalArgumentException("허용되지 않는 형식"));

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("허용되지 않는 형식"));
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
                .when(imageService).delete(" ");

        mockMvc.perform(delete("/api/images")
                        .param("key", " ")
                        .cookie(new Cookie("accessToken", token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("삭제할 사진이 없어요"));
    }
}
