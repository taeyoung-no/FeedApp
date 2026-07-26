package com.feedapp.server.storage;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/api/images")
    @ResponseStatus(HttpStatus.CREATED)
    public StoredImage upload(@RequestParam("file") MultipartFile file) throws IOException {
        return imageService.upload(
                file.getInputStream(),
                file.getContentType(),
                file.getSize()
        );
    }

    @DeleteMapping("/api/images")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam("key") String key) {
        imageService.delete(key);
    }
}
