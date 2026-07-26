package com.feedapp.server.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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
    @DisplayName("유효한 요청이면 업로드 성공 (jpg)")
    void upload() {
        final var content = new ByteArrayInputStream(new byte[] {1, 2, 3});
        final var stored = new StoredImage(
                "posts/image.jpg",
                "https://feedapp-photos.s3.ap-northeast-2.amazonaws.com/posts/image.jpg"
        );
        when(imageStorage.upload(anyString(), any(), eq("image/jpeg"), eq(3L)))
                .thenReturn(stored);

        final StoredImage result = imageService.upload(content, "image/jpeg", 3L);

        assertThat(result).isEqualTo(stored);

        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).upload(keyCaptor.capture(), any(), eq("image/jpeg"), eq(3L));
        assertThat(keyCaptor.getValue()).startsWith("posts/");
        assertThat(keyCaptor.getValue()).endsWith(".jpg");
    }

    @Test
    @DisplayName("유효한 요청이면 업로드 성공 (png)")
    void uploadPng() {
        final var content = new ByteArrayInputStream(new byte[] {1, 2, 3});
        final var stored = new StoredImage(
                "posts/image.png",
                "https://feedapp-photos.s3.ap-northeast-2.amazonaws.com/posts/image.png"
        );
        when(imageStorage.upload(anyString(), any(), eq("image/png"), eq(3L)))
                .thenReturn(stored);

        final StoredImage result = imageService.upload(content, "image/png", 3L);

        assertThat(result).isEqualTo(stored);

        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).upload(keyCaptor.capture(), any(), eq("image/png"), eq(3L));
        assertThat(keyCaptor.getValue()).startsWith("posts/");
        assertThat(keyCaptor.getValue()).endsWith(".png");
    }

    @Test
    @DisplayName("유효한 webp 요청이면 업로드 성공 (webp)")
    void uploadWebp() {
        final var content = new ByteArrayInputStream(new byte[] {1, 2, 3});
        final var stored = new StoredImage(
                "posts/image.webp",
                "https://feedapp-photos.s3.ap-northeast-2.amazonaws.com/posts/image.webp"
        );
        when(imageStorage.upload(anyString(), any(), eq("image/webp"), eq(3L)))
                .thenReturn(stored);

        final StoredImage result = imageService.upload(content, "image/webp", 3L);

        assertThat(result).isEqualTo(stored);

        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).upload(keyCaptor.capture(), any(), eq("image/webp"), eq(3L));
        assertThat(keyCaptor.getValue()).startsWith("posts/");
        assertThat(keyCaptor.getValue()).endsWith(".webp");
    }

    @Test
    @DisplayName("허용되지 않는 형식이면 업로드 실패")
    void uploadWhenUnsupportedType() {
        final var content = new ByteArrayInputStream(new byte[] {1});

        assertThatThrownBy(() -> imageService.upload(content, "application/pdf", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 형식");

        verify(imageStorage, never()).upload(anyString(), any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("contentType이 null이면 업로드 실패")
    void uploadWhenContentTypeNull() {
        final var content = new ByteArrayInputStream(new byte[] {1});

        assertThatThrownBy(() -> imageService.upload(content, null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 형식");

        verify(imageStorage, never()).upload(anyString(), any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("크기가 0 이하면 업로드 실패")
    void uploadWhenEmpty() {
        final var content = new ByteArrayInputStream(new byte[0]);

        assertThatThrownBy(() -> imageService.upload(content, "image/jpeg", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사진이 없는 듯");

        verify(imageStorage, never()).upload(anyString(), any(), anyString(), anyLong());
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

