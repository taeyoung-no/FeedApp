package com.feedapp.server.post;

import java.time.LocalDateTime;
import java.util.List;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public List<PostResponse> findAll() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public PostResponse create(String title, String content, String author) {
        Post saved = postRepository.save(new Post(null, title, content, author, LocalDateTime.now()));
        return toResponse(saved);
    }

    public void delete(Long id, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        if (!post.getAuthor().equals(username)) {
            throw new ForbiddenException("권한 없음");
        }
        postRepository.delete(post);
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt()
        );
    }
}
