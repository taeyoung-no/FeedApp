package com.feedapp.server.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    ImageStorage imageStorage;

    @InjectMocks
    ImageService imageService;

    @Test
    @DisplayName("유효한 요청이면 업로드용 presigned URL 발급 성공 (jpg)")
    void createUploadUrl() {
        when(imageStorage.createPresignedUploadUrl(anyString(), eq("image/jpeg")))
                .thenReturn("https://example.com/upload-url");

        final PresignedUpload result = imageService.createUploadUrl("image/jpeg");

        assertThat(result.uploadUrl()).isEqualTo("https://example.com/upload-url");

        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).createPresignedUploadUrl(keyCaptor.capture(), eq("image/jpeg"));
        assertThat(keyCaptor.getValue()).startsWith("posts/");
        assertThat(keyCaptor.getValue()).endsWith(".jpg");
        assertThat(result.key()).isEqualTo(keyCaptor.getValue());
    }

    @Test
    @DisplayName("유효한 요청이면 업로드용 presigned URL 발급 성공 (png)")
    void createUploadUrlPng() {
        when(imageStorage.createPresignedUploadUrl(anyString(), eq("image/png")))
                .thenReturn("https://example.com/upload-url");

        final PresignedUpload result = imageService.createUploadUrl("image/png");

        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).createPresignedUploadUrl(keyCaptor.capture(), eq("image/png"));
        assertThat(keyCaptor.getValue()).startsWith("posts/");
        assertThat(keyCaptor.getValue()).endsWith(".png");
        assertThat(result.key()).isEqualTo(keyCaptor.getValue());
        assertThat(result.uploadUrl()).isEqualTo("https://example.com/upload-url");
    }

    @Test
    @DisplayName("유효한 요청이면 업로드용 presigned URL 발급 성공 (webp)")
    void createUploadUrlWebp() {
        when(imageStorage.createPresignedUploadUrl(anyString(), eq("image/webp")))
                .thenReturn("https://example.com/upload-url");

        final PresignedUpload result = imageService.createUploadUrl("image/webp");

        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).createPresignedUploadUrl(keyCaptor.capture(), eq("image/webp"));
        assertThat(keyCaptor.getValue()).startsWith("posts/");
        assertThat(keyCaptor.getValue()).endsWith(".webp");
        assertThat(result.key()).isEqualTo(keyCaptor.getValue());
        assertThat(result.uploadUrl()).isEqualTo("https://example.com/upload-url");
    }

    @Test
    @DisplayName("허용되지 않는 형식이면 업로드 URL 발급 실패")
    void createUploadUrlWhenUnsupportedType() {
        assertThatThrownBy(() -> imageService.createUploadUrl("application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 형식");

        verify(imageStorage, never()).createPresignedUploadUrl(anyString(), anyString());
    }

    @Test
    @DisplayName("contentType이 null이면 업로드 URL 발급 실패")
    void createUploadUrlWhenContentTypeNull() {
        assertThatThrownBy(() -> imageService.createUploadUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 형식");

        verify(imageStorage, never()).createPresignedUploadUrl(anyString(), anyString());
    }

    @Test
    @DisplayName("유효한 키면 조회용 presigned URL 발급 성공")
    void createDownloadUrl() {
        when(imageStorage.createPresignedDownloadUrl("posts/image.jpg"))
                .thenReturn("https://example.com/download-url");

        final String result = imageService.createDownloadUrl("posts/image.jpg");

        assertThat(result).isEqualTo("https://example.com/download-url");
        verify(imageStorage).createPresignedDownloadUrl("posts/image.jpg");
    }

    @Test
    @DisplayName("키가 공백이면 조회 URL 발급 실패")
    void createDownloadUrlWhenBlankKey() {
        assertThatThrownBy(() -> imageService.createDownloadUrl(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사진이 없어요");

        verify(imageStorage, never()).createPresignedDownloadUrl(anyString());
    }

    @Test
    @DisplayName("키가 null이면 조회 URL 발급 실패")
    void createDownloadUrlWhenNullKey() {
        assertThatThrownBy(() -> imageService.createDownloadUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사진이 없어요");

        verify(imageStorage, never()).createPresignedDownloadUrl(anyString());
    }

    @Test
    @DisplayName("유효한 요청이면 삭제 성공")
    void delete() {
        imageService.delete("posts/image.jpg");

        verify(imageStorage).delete("posts/image.jpg");
    }

    @Test
    @DisplayName("키가 공백이면 삭제 실패")
    void deleteWhenBlankKey() {
        assertThatThrownBy(() -> imageService.delete(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제할 사진이 없어요");

        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("키가 null이면 삭제 실패")
    void deleteWhenNullKey() {
        assertThatThrownBy(() -> imageService.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제할 사진이 없어요");

        verify(imageStorage, never()).delete(anyString());
    }
}
