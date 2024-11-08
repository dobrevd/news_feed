package faang.school.postservice.service.like;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.event.LikeEvent;
import faang.school.postservice.kafka.EventsGenerator;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.mapper.LikeMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import faang.school.postservice.publisher.LikeEventPublisher;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.repository.LikeRepository;
import faang.school.postservice.service.comment.CommentService;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.validator.LikeValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static java.lang.Math.min;

@Slf4j
@Service
@Setter
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {
    @Value("${like-service.batch-size}")
    private int batchSize;
    private final LikeValidator likeValidator;
    private final LikeRepository likeRepository;
    private final LikeMapper likeMapper;
    private final LikeEventPublisher likePublisher;
    private final PostService postService;
    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final UserServiceClient userServiceClient;
    private final CommentRepository commentRepository;
    private final EventsGenerator eventsGenerator;

    @Override
    public void deleteCommentLike(LikeDto likeDto) {
        likeRepository.deleteByCommentIdAndUserId(likeDto.getCommentId(), likeDto.getUserId());
    }

    @Override
    @Transactional
    public LikeDto addLikeToComment(LikeDto likeDto) {
        Long userId = likeDto.getUserId();
        Long commentId = likeDto.getCommentId();

        likeValidator.validateUserExistence(userId);
        var comment = retrieveComment(commentId);
        likeValidator.validateLikeForComment(comment, userId);

        var like = addLikeToCommentEntity(likeDto, comment);

        publisher(userId, null, commentId, comment.getAuthorId());
        log.info("Like with likeId = {} added on comment with commentId = {} by user with userId = {}",
                like.getId(),
                commentId,
                userId);

        return likeMapper.toDto(like);
    }

    @Override
    @Transactional
    public LikeDto addLikeToPost(LikeDto likeDto) {
        var userId = likeDto.getUserId();
        var postId = likeDto.getPostId();
        var postDto = postService.getPost(postId);

        validateLikeRequest(likeDto, postDto);

        var like = createLike(likeDto);
        postService.addLikeByPostId(postId, like);

        publisher(userId, postId, null, postDto.getAuthorId());
        log.info("Like with likeId = {} added on postDto with postId = {} by user with userId = {}",
                like.getId(),
                postId,
                userId);

        eventsGenerator.generateAndSendLikeEvent(postDto);
        return likeMapper.toDto(like);
    }

    @Override
    @Transactional
    public void deleteLikeFromPost(LikeDto likeDto) {
        var userId = likeDto.getUserId();
        var postId = likeDto.getPostId();
        var like = likeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new NoSuchElementException("Like with postId: " + postId + " is not found."));

        postService.removeLikeByPostId(postId, like);
        likeRepository.deleteByPostIdAndUserId(postId, userId);
    }

    public List<UserDto> findUsersByPostId(Long postId) {
        var userIds = likeRepository.findByPostId(postId).stream()
                .map(Like::getUserId)
                .toList();
        checkUserIdListEmpty(userIds, postId);

        return getUsersInBatches(userIds);
    }

    public List<UserDto> findUsersByCommentId(Long commentId) {
        var userIds = likeRepository.findByCommentId(commentId).stream()
                .map(Like::getUserId)
                .toList();
        checkUserIdListEmpty(userIds, commentId);

        return getUsersInBatches(userIds);
    }

    private void publisher(Long userId, Long postId, Long commentId, Long authorId) {
        LikeEvent event = LikeEvent.builder()
                .authorLikeId(userId)
                .commentId(commentId)
                .postId(postId)
                .authorCommentId(commentId != null ? authorId : null)
                .authorPostId(commentId != null ? authorId : null)
                .completedAt(LocalDateTime.now())
                .build();
        likePublisher.publish(event);
    }

    private List<UserDto> getUsersInBatches(List<Long> userIds) {
        List<UserDto> allUsers = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i += batchSize) {
            int end = min(i + batchSize, userIds.size());
            var batch = userIds.subList(i, end);
            var batchUsers = userServiceClient.getUsersByIds(batch);
            allUsers.addAll(batchUsers);
        }
        return allUsers;
    }

    private void checkUserIdListEmpty(List<Long> userIds, Long id) {
        if (userIds.isEmpty()) {
            throw new EntityNotFoundException("No users found for ID " + id);
        }
    }

    private void validateLikeRequest(LikeDto likeDto, PostDto postDto) {
        likeValidator.validateUserExistence(likeDto.getUserId());
        likeValidator.validateLikeToPost(postDto, likeDto.getUserId());
    }

    private Like createLike(LikeDto likeDto) {
        var like = likeMapper.toEntity(likeDto);
        return likeRepository.save(like);
    }

    private Comment retrieveComment(Long commentId){
        var commentDto = commentService.findCommentById(commentId);
        return commentMapper.toEntity(commentDto);
    }

    private Like addLikeToCommentEntity(LikeDto likeDto, Comment comment){
        var like = likeMapper.toEntity(likeDto);
        comment.getLikes().add(like);
        commentRepository.save(comment);
        return likeRepository.save(like);
    }
}