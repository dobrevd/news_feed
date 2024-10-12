package faang.school.postservice.redis.service;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.redis.mapper.PostCacheMapper;
import faang.school.postservice.redis.model.PostCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static faang.school.postservice.util.TestDataFactory.FEED_PREFIX;
import static faang.school.postservice.util.TestDataFactory.FOLLOWER_ID;
import static faang.school.postservice.util.TestDataFactory.FOLLOWER_IDS;
import static faang.school.postservice.util.TestDataFactory.FOLLOWER_POST_IDS;
import static faang.school.postservice.util.TestDataFactory.ID;
import static faang.school.postservice.util.TestDataFactory.MAX_FEED_SIZE;
import static faang.school.postservice.util.TestDataFactory.MOCK_FEED_SIZE;
import static faang.school.postservice.util.TestDataFactory.POSTS_PER_PAGE;
import static faang.school.postservice.util.TestDataFactory.PUBLISHED_AT;
import static faang.school.postservice.util.TestDataFactory.creatFeedDto;
import static faang.school.postservice.util.TestDataFactory.createPostCache;
import static faang.school.postservice.util.TestDataFactory.createPostDto;
import static faang.school.postservice.util.TestDataFactory.EXPECTED_SCORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedCacheServiceTest {
    @InjectMocks
    private FeedCacheService feedCacheService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ZSetOperations<String, Object> zSetOperations;
    @Mock
    private PostCacheService postCacheService;
    @Mock
    private PostCacheMapper postCacheMapper;
    @Captor
    ArgumentCaptor<PostDto> postDtoCaptor;
    @Captor
    ArgumentCaptor<Double> scoreCaptor;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(feedCacheService, "maxFeedSize", MAX_FEED_SIZE);
        ReflectionTestUtils.setField(feedCacheService, "feedPrefix", FEED_PREFIX);
        ReflectionTestUtils.setField(feedCacheService, "postsPerPage", POSTS_PER_PAGE);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void shouldAddPostToFollowersFeedAndTrimFeedWhenFeedSizeExceeded() {
        // given - precondition
        when(zSetOperations.zCard(anyString())).thenReturn(MOCK_FEED_SIZE);

        // when - action
        feedCacheService.distributePostToFollowers(ID, FOLLOWER_IDS, PUBLISHED_AT);

        // then - verify the output
        FOLLOWER_IDS.forEach(followerId -> {
            verify(zSetOperations, times(1))
                    .add(eq(FEED_PREFIX + followerId), eq(ID), eq(EXPECTED_SCORE));
        });

        FOLLOWER_IDS.forEach(followerId -> {
            verify(zSetOperations, times(1))
                    .zCard(eq(FEED_PREFIX + followerId));
        });

        verify(redisTemplate, times(FOLLOWER_IDS.size() * 2)).opsForZSet();
        verifyNoMoreInteractions(redisTemplate);
        verifyNoMoreInteractions(zSetOperations);
    }

    @Test
    void givenUserIdAndPostIdWhenGetFeedByUserIdThenReturnFeed() {
        // given - precondition
        long rank = 12L;
        var postCacheList = List.of(createPostCache());
        var expectedPostDto = createPostDto();

        when(postCacheService.getPostCacheByIds(anyList())).thenReturn(postCacheList);
        when(postCacheMapper.toDto(any(PostCache.class))).thenReturn(expectedPostDto);
        when(zSetOperations.rank(eq(FEED_PREFIX + ID), eq(ID))).thenReturn(rank);
        when(zSetOperations.range(eq(FEED_PREFIX + ID), eq(rank + 1), eq(rank + POSTS_PER_PAGE)))
                .thenReturn(FOLLOWER_POST_IDS);

        // when - action
        var actualResult = feedCacheService.getFeedByUserId(ID, ID);

        // then - verify the output
        assertThat(actualResult).isNotNull();
        assertThat(actualResult.get(0)).usingRecursiveComparison()
                .isEqualTo(expectedPostDto);

        verify(postCacheService, times(1)).getPostCacheByIds(anyList());
        verify(postCacheMapper, times(1)).toDto(any(PostCache.class));
        verify(zSetOperations, times(1)).rank(eq(FEED_PREFIX + ID), eq(ID));
        verify(zSetOperations, times(1))
                .range(eq(FEED_PREFIX + ID), eq(rank + 1), eq(rank + POSTS_PER_PAGE));
    }

    @Test
    void shouldSaveFeedWhenSaveUserFeedHeat() {
        // given - precondition
        var feedDto = creatFeedDto();

        // when - action
        feedCacheService.saveUserFeedHeat(feedDto);

        // then - verify the output
        verify(redisTemplate.opsForZSet(), times(feedDto.posts().size()))
                .add(eq(FEED_PREFIX + FOLLOWER_ID), postDtoCaptor.capture(), scoreCaptor.capture());

        List<PostDto> capturedPosts = postDtoCaptor.getAllValues();
        List<Double> capturedScores = scoreCaptor.getAllValues();

        assertThat(capturedPosts).containsExactlyElementsOf(feedDto.posts());
        assertThat(capturedScores).allMatch(score -> score.equals(EXPECTED_SCORE));
    }
}