package faang.school.postservice.service.post;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.kafka.EventsGenerator;
import faang.school.postservice.mapper.PostMapper;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.redis.service.AuthorCacheService;
import faang.school.postservice.redis.service.PostCacheService;
import faang.school.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.time.LocalDateTime.now;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final EventsGenerator eventsGenerator;
    private final AuthorCacheService authorCacheService;
    private final PostCacheService postCacheService;

    public PostDto createPost(PostDto postDto) {
        var post = postMapper.toEntity(postDto);
        var savedPost = postRepository.save(post);

        return postMapper.toDto(savedPost);
    }

    public PostDto publishPost(Long postId) {
        var post = getPostByIdOrFail(postId);
        postPublishingValidation(post);

        var postDto = updateAndSavePost(post);
        cachePostAndNotifyFollowersAsync(postDto);

        return postDto;
    }

    public PostDto updatePost(final long postId, final PostDto postDto) {
        Post newPost = postMapper.toEntity(postDto);
        Post post = getPostByIdOrFail(postId);

        post.setContent(newPost.getContent());
        post.setUpdatedAt(now());

        return postMapper.toDto(postRepository.save(post));
    }


    public void deletePost(final long postId) {
        var post = getPostByIdOrFail(postId);

        post.setDeleted(true);
        post.setUpdatedAt(now());

        postRepository.save(post);
    }

    public PostDto getPost(final long postId) {
        var post = getPostByIdOrFail(postId);
        var postDto = postMapper.toDto(post);

        eventsGenerator.generateAndSendPostViewEvent(postDto);
        return postDto;
    }

    public List<PostDto> getPostsByIds(List<Long> postIds) {
        return postRepository.findAllById(postIds).stream()
                .map(postMapper::toDto)
                .toList();
    }

    public List<PostDto> getFilteredPosts(Long authorId, Long projectId, Boolean isPostPublished) {
        if (authorId != null) {
            return findByAuthor(authorId, isPostPublished);
        }
        if (projectId != null) {
            return findByProject(projectId, isPostPublished);
        }
        return Collections.emptyList();
    }

    public void addLikeByPostId(Long postId, Like like){
        var post = getPostByIdOrFail(postId);
        post.getLikes().add(like);
        postRepository.save(post);
    }

    public void removeLikeByPostId(Long postId, Like like) {
        var post = getPostByIdOrFail(postId);
        post.getLikes().remove(like);
        postRepository.save(post);
    }

    private Post getPostByIdOrFail(long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    private void postPublishingValidation(Post post) {
        if (post.isPublished()) {
            throw new IllegalArgumentException("Post is already published");
        }
    }

    private PostDto updateAndSavePost(Post post){
        var now = now();
        post.setPublished(true);
        post.setPublishedAt(now);
        post.setUpdatedAt(now);

        var savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    private CompletableFuture<Void> cachePostAndNotifyFollowersAsync(PostDto postDto){
        var cacheAuthorFuture = runAsync(() -> authorCacheService.saveAuthorCache(postDto.getAuthorId()));
        var cachePostFuture = supplyAsync(() -> postCacheService.savePostCache(postDto));
        var triggerEventFuture = runAsync(() -> eventsGenerator.generateAndSendPostFollowersEvent(postDto));

        return allOf(cacheAuthorFuture, cachePostFuture, triggerEventFuture);
    }

    private List<PostDto> findByAuthor(Long authorId, Boolean isPostPublished) {
        return postRepository.findByAuthorIdAndPublishedAndDeletedIsFalseOrderByPublished(authorId, isPostPublished)
                .stream()
                .map(postMapper::toDto)
                .toList();
    }

    private List<PostDto> findByProject(Long projectId, Boolean isPostPublished) {
        return postRepository.findByProjectIdAndPublishedAndDeletedIsFalseOrderByPublished(projectId, isPostPublished)
                .stream()
                .map(postMapper::toDto)
                .toList();
    }
}