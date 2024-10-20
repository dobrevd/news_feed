package faang.school.postservice.redis.service;

import faang.school.postservice.redis.mapper.PostCacheMapper;
import faang.school.postservice.redis.model.PostCache;
import faang.school.postservice.redis.repository.PostCacheRepository;
import faang.school.postservice.service.post.PostRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static faang.school.postservice.util.TestDataFactory.COMMENT_LIMIT_PER_POST;
import static faang.school.postservice.util.TestDataFactory.ID;
import static faang.school.postservice.util.TestDataFactory.POST_CACHE_KEY_PREFIX;
import static faang.school.postservice.util.TestDataFactory.POST_CACHE_LIKES_FIELD;
import static faang.school.postservice.util.TestDataFactory.POST_CACHE_VIEWS_FIELD;
import static faang.school.postservice.util.TestDataFactory.createComment;
import static faang.school.postservice.util.TestDataFactory.createPostCache;
import static faang.school.postservice.util.TestDataFactory.createPostDto;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCacheServiceTest {
    @InjectMocks
    private PostCacheService postCacheService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private PostCacheRepository postCacheRepository;
    @Mock
    private PostCacheMapper postCacheMapper;
    @Mock
    private PostRetrievalService postRetrievalService;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @BeforeEach
    void setup(){
        ReflectionTestUtils.setField(postCacheService, "postCacheViewsField", POST_CACHE_VIEWS_FIELD);
        ReflectionTestUtils.setField(postCacheService, "postCacheLikesField", POST_CACHE_LIKES_FIELD);
        ReflectionTestUtils.setField(postCacheService, "postCacheKeyPrefix", POST_CACHE_KEY_PREFIX);
        ReflectionTestUtils.setField(postCacheService, "commentLimitPerPost", COMMENT_LIMIT_PER_POST);

        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void shouldIncrementPostViewsInCache() {
        // given - precondition
        when(postCacheRepository.existsById(ID)).thenReturn(true);
        when(hashOperations.increment(anyString(), anyString(), eq(1L))).thenReturn(1L);

        // when - action
        postCacheService.incrementConcurrentPostViews(ID);

        // then - verify the output
        verify(postCacheRepository, times(1)).existsById(ID);
        verify(hashOperations, times(1)).increment(anyString(), anyString(), eq(1L));

        verifyNoInteractions(postRetrievalService);
        verifyNoInteractions(postCacheMapper);
        verifyNoMoreInteractions(postCacheRepository);
    }

    @Test
    void shouldIncrementPostLikesInCache() {
        // given - precondition
        when(postCacheRepository.existsById(ID)).thenReturn(true);
        when(hashOperations.increment(anyString(), anyString(), eq(1L))).thenReturn(1L);

        // when - action
        postCacheService.incrementConcurrentPostLikes(ID);

        // then - verify the output
        verifyNoInteractions(postRetrievalService);
        verifyNoInteractions(postCacheMapper);
        verifyNoMoreInteractions(postCacheRepository);
    }

    @Test
    void shouldAddCommentToCachePost() {
        // given - precondition
        var postCache = createPostCache();
        var comment = createComment();

        when(postCacheRepository.findById(ID)).thenReturn(of(postCache));

        // when - action
        postCacheService.addCommentToCachedPost(ID, comment);

        // then - verify the output
        assertThat(postCache.getComments()).contains(comment);

        verify(postCacheRepository, times(1)).findById(ID);

        verifyNoInteractions(postRetrievalService);
        verifyNoInteractions(postCacheMapper);
    }


    @Test
    void shouldReturnPostCachesForGivenPostIds() {
        // given - precondition
        var postIds = List.of(23L);
        var expectedResult = List.of(createPostCache());

        when(postCacheRepository.findAllById(anyList())).thenReturn(expectedResult);

        // when - action
        var actualResult = postCacheService.getPostCacheByIds(postIds);

        // then - verify the output
        assertThat(actualResult).isNotEmpty();
        assertThat(actualResult.size()).isEqualTo(expectedResult.size());
        assertThat(actualResult).containsExactlyElementsOf(expectedResult);

        verify(postCacheRepository, times(1)).findAllById(postIds);
    }

    @Test
    void shouldSavePostCache() {
        // given - precondition
        var postDto = createPostDto();
        var postCache = createPostCache();

        when(postCacheMapper.toPostCache(postDto)).thenReturn(postCache);
        when(postCacheRepository.save(any(PostCache.class))).thenReturn(postCache);

        // when - action
        var actualResult = postCacheService.savePostCache(postDto);

        // then - verify the output
        assertThat(actualResult).isNotNull();
        assertThat(actualResult).usingRecursiveComparison().isEqualTo(postCache);
    }
}