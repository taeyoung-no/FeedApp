package com.feedapp.server.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.like.PostLikeRepository;
import com.feedapp.server.storage.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int CONTENT_MAX_LENGTH = 500;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int POPULAR_DAYS = 1;
    private static final int POPULAR_SIZE = 10;

    private final PostRepository postRepository;
    private final ImageService imageService;
    private final PostLikeRepository postLikeRepository;
    private final PopularPostCache popularPostCache;

    public CursorPage<PostResponse> findAll(String username, String cursor, int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("잘못된 크기");
        }
        PostWindow window = postRepository.findAllWithImages(cursor, size);
        List<PostResponse> content = window.content().stream()
                .map((post) -> toResponse(post, username))
                .toList();
        String nextCursor = null;
        String prevCursor = null;
        if (window.hasNext()) {
            Post last = window.content().getLast();
            nextCursor = new PostListCursor(last.getCreatedAt(), last.getId(), false).encode();
        }
        if (window.hasPrevious()) {
            Post first = window.content().getFirst();
            prevCursor = new PostListCursor(first.getCreatedAt(), first.getId(), true).encode();
        }
        return new CursorPage<>(content, nextCursor, prevCursor, window.hasNext(), window.hasPrevious());
    }

    public List<PostResponse> findPopular(String username) {
        List<PopularPost> posts = popularPostCache.getOrLoad(() -> {
            LocalDateTime since = LocalDateTime.now().minusDays(POPULAR_DAYS);
            return postRepository.findPopularWithImages(since, POPULAR_SIZE).stream()
                    .map(PopularPost::from)
                    .toList();
        });
        return posts.stream()
                .map((post) -> toResponse(post, username))
                .toList();
    }

    public PostResponse findById(Long id, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        return toResponse(post, username);
    }

    public PostResponse create(String title, String content, String author, List<String> imageKeys) {
        validateContent(content);
        Post post = new Post(null, title, content, author, LocalDateTime.now(), new ArrayList<>());
        addImages(post, imageKeys);
        Post saved = postRepository.save(post);
        return toResponse(saved, null);
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
                new ArrayList<>(),
                post.getLikeCount()
        );
        addImages(updated, imageKeys);
        return toResponse(postRepository.save(updated), username);
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

    private PostResponse toResponse(Post post, String username) {
        List<String> imageKeys = post.getImages().stream()
                .map(PostImage::getImageKey)
                .toList();
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt(),
                toImageResponses(imageKeys),
                isLiked(username, post.getId()),
                post.getLikeCount()
        );
    }

    private PostResponse toResponse(PopularPost post, String username) {
        return new PostResponse(
                post.id(),
                post.title(),
                post.content(),
                post.author(),
                post.createdAt(),
                toImageResponses(post.imageKeys()),
                isLiked(username, post.id()),
                post.likeCount()
        );
    }

    private List<PostImageResponse> toImageResponses(List<String> imageKeys) {
        return imageKeys.stream()
                .map((imageKey) -> new PostImageResponse(imageKey, imageService.createDownloadUrl(imageKey)))
                .toList();
    }

    private boolean isLiked(String username, Long postId) {
        return username != null && postLikeRepository.existsByMemberUsernameAndPostId(username, postId);
    }
}
