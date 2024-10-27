package faang.school.postservice.service.comment;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.kafka.EventsGenerator;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.redis.service.AuthorCacheService;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.comment.error.CommentServiceErrors;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static java.time.LocalDateTime.*;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository repository;
    private final PostRepository postRepository;
    private final CommentMapper mapper;
    private final EventsGenerator eventsGenerator;
    private final AuthorCacheService authorCacheService;

    @Transactional
    public CommentDto addCommentToPost(Long postId, CommentDto commentDto) {
        var post = validatePostExists(postId);

        var savedComment = createAndSaveComment(post, commentDto);
        updatePostWithComment(post, savedComment);

        var savedCommentDto = mapper.toDto(savedComment);

        eventsGenerator.generateAndSendCommentEvent(savedCommentDto);
        authorCacheService.saveAuthorCache(savedCommentDto.getAuthorId());

        return savedCommentDto;
    }

    public CommentDto updateCommentOnPost(Long postId, CommentDto commentDto) {
        validatePostExists(postId);
        if (!repository.existsById(commentDto.getId())){
            throw new IllegalArgumentException(CommentServiceErrors.COMMENT_NOT_FOUND.getValue());
        }

        var savedComment = saveComment(commentDto);
        return mapper.toDto(savedComment);
    }

    public List<CommentDto> findCommentsByPostId(Long postId) {
        validatePostExists(postId);

        return repository.findAllByPostId(postId)
                .stream()
                .map(mapper::toDto)
                .sorted(this::localDateComparator)
                .toList();
    }

    public CommentDto findCommentById(Long commentId) {
        var comment = findExistingComment(commentId);
        return mapper.toDto(comment);
    }

    public CommentDto deleteComment(Long postId, CommentDto commentDto) {
        validatePostExists(postId);
        repository.deleteById(commentDto.getId());
        return commentDto;
    }

    private Post validatePostExists(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException(CommentServiceErrors.POST_NOT_FOUND.getValue()));
    }

    private Comment createAndSaveComment(Post post, CommentDto commentDto){
        var comment = mapper.toEntity(commentDto);
        comment.setPost(post);
        return repository.save(comment);
    }

    private void updatePostWithComment(Post post, Comment comment){
        post.getComments().add(comment);
        post.setUpdatedAt(now());
        postRepository.save(post);
    }

    private Comment findExistingComment(Long commentId) {
        return repository.findById(commentId).
                orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    }

    private Comment saveComment(CommentDto commentDto){
        commentDto.setUpdatedAt(now());
        var comment = mapper.toEntity(commentDto);
        return repository.save(comment);
    }

    private int localDateComparator(CommentDto commentLeft, CommentDto commentRight) {
        return Comparator.comparing(CommentDto::getCreatedAt)
                .compare(commentLeft, commentRight);
    }
}