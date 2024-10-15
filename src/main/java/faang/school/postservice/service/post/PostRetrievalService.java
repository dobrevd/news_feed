package faang.school.postservice.service.post;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.mapper.PostMapper;
import faang.school.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostRetrievalService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public PostDto getPostById(Long postId){
        return postRepository.findById(postId)
                .map(postMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }
}
