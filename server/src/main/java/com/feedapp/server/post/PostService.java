package com.feedapp.server.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.storage.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int CONTENT_MAX_LENGTH = 500;

    private final PostRepository postRepository;
    private final ImageService imageService;

    public List<PostResponse> findAll() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public PostResponse findById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        return toResponse(post);
    }

    public PostResponse create(String title, String content, String author, List<String> imageKeys) {
        validateContent(content);
        Post post = new Post(null, title, content, author, LocalDateTime.now(), new ArrayList<>());
        addImages(post, imageKeys);
        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    public void delete(Long id, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        if (!post.getAuthor().equals(username)) {
            throw new ForbiddenException("권한 없음");
        }
        List<String> imageKeys = post.getImages().stream()
                .map(PostImage::getImageKey)
                .toList();
        for (String imageKey : imageKeys) {
            imageService.delete(imageKey);
        }
        postRepository.delete(post);
    }

    public PostResponse update(Long id, String title, String content, String username, List<String> imageKeys) {
        validateContent(content);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        if (!post.getAuthor().equals(username)) {
            throw new ForbiddenException("권한 없음");
        }
        Post updated = new Post(
                id,
                title,
                content,
                post.getAuthor(),
                post.getCreatedAt(),
                new ArrayList<>()
        );
        addImages(updated, imageKeys);
        return toResponse(postRepository.save(updated));
    }

    private void validateContent(String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("내용 입력하세요");
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("너무 길어요");
        }
    }

    private void addImages(Post post, List<String> imageKeys) {
        if (imageKeys == null) {
            return;
        }
        for (int i = 0; i < imageKeys.size(); i++) {
            post.getImages().add(new PostImage(null, post, imageKeys.get(i), i));
        }
    }

    private PostResponse toResponse(Post post) {
        List<PostImageResponse> images = post.getImages().stream()
                .map((image) -> new PostImageResponse(
                        image.getImageKey(),
                        imageService.createDownloadUrl(image.getImageKey())
                ))
                .toList();
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt(),
                images
        );
    }
}
