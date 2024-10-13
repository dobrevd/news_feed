package faang.school.postservice.controller.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import faang.school.postservice.controller.PostController;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.service.post.PostService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService service;

    @InjectMocks
    private PostController controller;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testCreate() throws Exception {
        // Arrange
        PostDto postDto = PostMock.generatePostDto(PostMock.authorId, PostMock.projectId, false, PostMock.content);

        when(service.createPost(postDto)).thenReturn(postDto);

        // Act & Assert
        mockMvc.perform(post("/posts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.is(PostMock.content)));
    }

    @Test
    public void testPublish() throws Exception {
        // Arrange
        PostDto postDto = PostMock.generatePostDto(PostMock.authorId, null, true, PostMock.content);

        when(service.publishPost(PostMock.postId)).thenReturn(postDto);

        // Act & Assert
        mockMvc.perform(put("/posts/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published", is(true)));
    }

    @Test
    public void testUpdate() throws Exception {
        // Arrange
        PostDto postDto = PostMock.generatePostDto(PostMock.authorId, PostMock.projectId, true, PostMock.newContent);

        when(service.updatePost(PostMock.postId, postDto)).thenReturn(postDto);

        // Act & Assert
        mockMvc.perform(patch("/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.is(PostMock.newContent)));
    }

    @Test
    public void testDelete() throws Exception {
        mockMvc.perform(delete("/posts/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testGet() throws Exception {
        // Arrange
        PostDto postDto = PostMock.generatePostDto(PostMock.authorId, null, true, PostMock.content);

        when(service.getPost(PostMock.postId)).thenReturn(postDto);

        // Act & Assert
        mockMvc.perform(get("/posts/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    public void testGetFiltered() throws Exception {
        // Arrange
        List<PostDto> posts = PostMock.generateFilteredPostsDto(PostMock.authorId, null, true);

        when(service.getFilteredPosts(PostMock.authorId, null, true)).thenReturn(posts);

        // Act & Assert
        mockMvc.perform(get("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("authorId", "1")
                        .param("published", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}