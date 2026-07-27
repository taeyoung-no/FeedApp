package com.feedapp.server.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/api/images/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public PresignedUpload createUploadUrl(@RequestBody CreateUploadUrlRequest request) {
        return imageService.createUploadUrl(request.contentType());
    }

    @GetMapping("/api/images/download-url")
    @ResponseStatus(HttpStatus.OK)
    public PresignedDownload createDownloadUrl(@RequestParam("key") String key) {
        return new PresignedDownload(imageService.createDownloadUrl(key));
    }

    @DeleteMapping("/api/images")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam("key") String key) {
        imageService.delete(key);
    }
}
