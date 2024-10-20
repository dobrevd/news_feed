package faang.school.postservice.util;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.kafka.events.FeedDto;
import faang.school.postservice.redis.model.AuthorCache;
import faang.school.postservice.redis.model.PostCache;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Long.MAX_VALUE;
import static java.util.List.of;

@UtilityClass
public final class TestDataFactory {

    public static final Long ID = 1L;
    public static final Long POST_AUTHOR_ID = 1L;

    public static final Long INVALID_ID = MAX_VALUE;

    public final static String POST_CACHE_VIEWS_FIELD = "view";
    public final static String POST_CACHE_LIKES_FIELD = "likes";
    public final static String POST_CACHE_KEY_PREFIX = "post:";
    public final static int COMMENT_LIMIT_PER_POST = 3;


    public static final Long FOLLOWER_ID = 111L;
    public static final List<Long> FOLLOWER_IDS = of(2L, 3L, 4L);
    public static final Set<Object> FOLLOWER_POST_IDS = Set.of(22L, 33L, 44L);

    public static final int MAX_FEED_SIZE = 500;
    public static final String FEED_PREFIX = "feed:";
    public static final int POSTS_PER_PAGE = 20;
    public static final Long MOCK_FEED_SIZE = 500L;
    public final static int MAX_POSTS_IN_HEAT_FEED = 500;

    public static final LocalDateTime PUBLISHED_AT = LocalDateTime.now().minusMonths(3);
    public static final double EXPECTED_SCORE = (double) PUBLISHED_AT.toInstant(ZoneOffset.UTC).toEpochMilli();

    public static List<UserDto> getUserDtoList() {
        var userAlex = UserDto.builder()
                .id(1L)
                .username("Alex")
                .email("alex@gmail.com")
                .build();

        var userAnna = UserDto.builder()
                .id(2L)
                .username("Anna")
                .email("anna@gmail.com")
                .build();

        var userOlga = UserDto.builder()
                .id(3L)
                .username("Olga")
                .email("olga@gmail.com")
                .build();

        return of(userOlga, userAnna, userAlex);
    }


    public static PostDto createPostDto() {
        var comment = createComment();
        return PostDto.builder()
                .id(123L)
                .content("Content")
                .authorId(12L)
                .likes(8)
                .views(100)
                .comments(of(comment))
                .publishedAt(PUBLISHED_AT)
                .build();
    }

    public static PostCache createPostCache(){
        var comment = createComment();
        var comments = new CopyOnWriteArrayList<CommentDto>();
        comments.add(comment);
        return PostCache.builder()
                .id(123L)
                .content("Content")
                .authorId(12L)
                .likes(8)
                .views(100)
                .comments(comments)
                .build();
    }

    public static CommentDto createComment(){
        return CommentDto.builder()
                .id(1L)
                .content("Comment1")
                .build();
    }

    public static List<UserDto> createUserDtoList() {
        var userDto = createUserDto();
        return of(userDto);
    }

    public static UserDto createUserDto(){
        return UserDto
                .builder()
                .id(12345L)
                .username("testUserName")
                .email("test@email.com")
                .followees(of(888L))
                .posts(of(ID))
                .build();
    }

    public static AuthorCache createAuthorCache() {
        return new AuthorCache(12345L, "testUserName", "test@email.com");
    }

    public static FeedDto creatFeedDto(){
        var postDto = createPostDto();
        return new FeedDto(111L, of(postDto));
    }
}
