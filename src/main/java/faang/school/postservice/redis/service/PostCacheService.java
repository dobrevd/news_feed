package faang.school.postservice.redis.service;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.redis.mapper.PostCacheMapper;
import faang.school.postservice.redis.model.PostCache;
import faang.school.postservice.redis.repository.PostCacheRepository;
import faang.school.postservice.service.post.PostRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class PostCacheService {
    @Value("${spring.data.redis.post-cache.views}")
    private String postCacheViewsField;
    @Value("${spring.data.redis.post-cache.likes}")
    private String postCacheLikesField;
    @Value("${spring.data.redis.post-cache.key-prefix}")
    private String postCacheKeyPrefix;
    @Value("${spring.data.redis.post-cache.comments-per-post:3}")
    private int commentLimitPerPost;

    @Qualifier("redisCacheTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostCacheRepository postCacheRepository;
    private final PostCacheMapper postCacheMapper;
    private final PostRetrievalService postRetrievalService;

    public PostCacheService(RedisTemplate<String, Object> redisTemplate, PostCacheRepository postCacheRepository,
                            PostCacheMapper postCacheMapper, PostRetrievalService postRetrievalService) {
        this.redisTemplate = redisTemplate;
        this.postCacheRepository = postCacheRepository;
        this.postCacheMapper = postCacheMapper;
        this.postRetrievalService = postRetrievalService;
    }

    public void incrementConcurrentPostViews(Long postId){
        if (postCacheRepository.existsById(postId)) {
            redisTemplate.opsForHash()
                    .increment(generateCachePostKey(postId), postCacheViewsField, 1);
        }else {
            var postDto = postRetrievalService.getPostById(postId);
            savePostCache(postDto);
        }
    }

    public void incrementConcurrentPostLikes(Long postId) {
        if (postCacheRepository.existsById(postId)){
            redisTemplate.opsForHash()
                    .increment(generateCachePostKey(postId), postCacheLikesField, 1);
        } else {
            var postDto = postRetrievalService.getPostById(postId);
            savePostCache(postDto);
        }
    }

    public void addCommentToCachedPost(Long postId, CommentDto commentDto) {
        var postCache = postCacheRepository.findById(postId)
                .orElseGet(() -> createAndCachePost(postId));

        if (postCache != null) {
            verifyAndAddComment(postCache, commentDto);
        }
    }

    public List<PostCache> getPostCacheByIds(List<Long> postIds) {
        var iterable = postCacheRepository.findAllById(postIds);
        return StreamSupport.stream(iterable.spliterator(), false)
                .toList();
    }

    public PostCache savePostCache(PostDto postDto) {
        var postCache = postCacheMapper.toPostCache(postDto);
        return postCacheRepository.save(postCache);
    }

    private String generateCachePostKey(Long postId) {
        return postCacheKeyPrefix + postId;
    }

    private PostCache createAndCachePost(Long postId) {
        var postDto = postRetrievalService.getPostById(postId);
        return savePostCache(postDto);
    }

    private void verifyAndAddComment(PostCache postCache, CommentDto commentDto) {
        CopyOnWriteArrayList<CommentDto> comments = postCache.getComments();
        removeOldestIfFull(comments);
        addCommentIfNotPresent(comments, commentDto);
    }

    private void removeOldestIfFull(CopyOnWriteArrayList<CommentDto> comments) {
        if (comments.size() == commentLimitPerPost) {
            comments.remove(comments.size() - 1);
        }
    }

    private void addCommentIfNotPresent(CopyOnWriteArrayList<CommentDto> comments, CommentDto commentDto){
        if (!comments.contains(commentDto)){
            comments.add(0, commentDto);
        }
    }
}