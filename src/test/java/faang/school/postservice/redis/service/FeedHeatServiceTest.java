package faang.school.postservice.redis.service;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.kafka.events.FeedDto;
import faang.school.postservice.kafka.producer.KafkaEventProducer;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static faang.school.postservice.util.TestDataFactory.*;
import static faang.school.postservice.util.TestDataFactory.MAX_POSTS_IN_HEAT_FEED;
import static faang.school.postservice.util.TestDataFactory.createPostDto;
import static faang.school.postservice.util.TestDataFactory.createUserDtoList;
import static java.util.List.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedHeatServiceTest {
    @InjectMocks
    private FeedHeatService feedHeatService;
    @Mock
    private KafkaEventProducer kafkaEventProducer;
    @Mock
    private AuthorCacheService authorCacheService;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private PostService postService;

    @BeforeEach
    void init(){
        ReflectionTestUtils.setField(feedHeatService, "maxPostsInHeatFeed", MAX_POSTS_IN_HEAT_FEED);
    }

    @Test
    void whenSendHeatEventsThenAuthorCachesSavedAndPostHeatEventsSent() {
        // given - precondition
        mockUserServiceAndCacheBehavior();
        mockPostServiceBehavior();
        mockKafkaProducerBehavior();

        // when - action
        feedHeatService.sendHeatEvents();

        // then - verify the output
        verifyUserServiceInteractions();
        verifyPostServiceInteractions();
        verifyKafkaProducerInteractions();
    }

    private void mockUserServiceAndCacheBehavior() {
        var userDtoList = createUserDtoList();
        when(userServiceClient.getAllUsers()).thenReturn(userDtoList);
        when(authorCacheService.saveAllAuthorsInCache(anyList())).thenReturn(completedFuture(null));
        when(userServiceClient.getUsersByIds(anyList())).thenReturn(userDtoList);
    }

    private void mockPostServiceBehavior() {
        var postDto = createPostDto();
        when(postService.getPostsByIds(anyList())).thenReturn(of(postDto));
    }

    private void mockKafkaProducerBehavior() {
        doNothing().when(kafkaEventProducer).sendFeedHeatEvent(any(FeedDto.class));
    }

    private void verifyUserServiceInteractions() {
        verify(userServiceClient, times(1)).getAllUsers();
        verify(authorCacheService, times(1)).saveAllAuthorsInCache(anyList());
        verify(userServiceClient, times(1)).getUsersByIds(anyList());
    }

    private void verifyPostServiceInteractions() {
        verify(postService, times(createUserDtoList().size())).getPostsByIds(anyList());
    }

    private void verifyKafkaProducerInteractions() {
        verify(kafkaEventProducer).sendFeedHeatEvent(any(FeedDto.class));
    }
}