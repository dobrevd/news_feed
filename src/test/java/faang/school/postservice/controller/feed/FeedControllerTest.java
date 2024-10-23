package faang.school.postservice.controller.feed;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.redis.service.FeedCacheService;
import faang.school.postservice.redis.service.FeedHeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static faang.school.postservice.util.TestDataFactory.createPostDto;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FeedControllerTest {
    @InjectMocks
    private FeedController feedController;
    @Mock
    private FeedCacheService feedCacheService;
    @Mock
    private FeedHeatService feedHeatService;
    @Mock
    private UserContext userContext;

    private MockMvc mockMvc;

    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(feedController).build();
    }

    @Test
    void givenPostIdWhenGetUserFeedWhenReturnPosts() throws Exception {
        // given - precondition
        var userId = 123L;
        var postDtos = List.of(createPostDto());

        when(userContext.getUserId()).thenReturn(userId);
        when(feedCacheService.getFeedByUserId(anyLong(), eq(userId))).thenReturn(postDtos);

        // when - action
        var response = mockMvc.perform(get("/api/feed")
                .param("postId", "1"));

        // then - verify the output
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(postDtos.size()))
                .andDo(print());
    }

    @Test
    void shouldSendHeatEventsAsyncSuccessfully() throws Exception {
        // given - precondition

        doNothing().when(feedHeatService).sendHeatEvents();

        // when - action
        var response = mockMvc.perform(get("/api/heat"));

        // then - verify the output
        response.andExpect(status().isOk())
                .andDo(print());

        verify(feedHeatService, times(1)).sendHeatEvents();
    }
}