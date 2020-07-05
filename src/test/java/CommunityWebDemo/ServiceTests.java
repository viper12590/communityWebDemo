package CommunityWebDemo;

import CommunityWebDemo.entity.Post;
import CommunityWebDemo.repository.PostRepository;
import CommunityWebDemo.repository.UserRepository;
import CommunityWebDemo.service.PostService;
import CommunityWebDemo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class ServiceTests {

    @Autowired
    PostRepository postRepository;
    @Autowired
    PostService postService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserService userService;

    @Test
    void addPostTest() {
        postRepository.deleteAll();
        Post post0 = new Post("title", "body");
        Post post1 = new Post("title2","body");
        List<Post> result = new ArrayList<>();
        postService.add(post0);
        postService.add(post1);
        postRepository.findAll().forEach(result::add);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void updatePostTest() {
        postRepository.deleteAll();
        List<Post> result = new ArrayList<>();
        postService.add(new Post("update this title","body"));
        postService.add(new Post("don't edit this title","body"));
        postRepository.findAll().forEach(result::add);
        Post postEdit = new Post();
        for(Post post : result) {
            if(post.getTitle().equals("update this title")) {
                postEdit = post;
                break;
            }
        }
        assertThat(postEdit.getTitle()).isEqualTo("update this title");
        Long editId = postEdit.getId();
        postEdit.setTitle("new title");
        postService.add(postEdit);
        assertThat(postRepository.findById(editId).isPresent()).isTrue();
        assertThat(postRepository.findById(editId).get().getTitle()).isEqualTo("new title");
    }

    @Test
    void getAllPostTest() {
        postRepository.deleteAll();
        postRepository.save(new Post("post1","body"));
        postRepository.save(new Post("post2","body"));
        postRepository.save(new Post("post3","body"));
        assertThat(postService.getAll().size()).isEqualTo(3);
    }

    @Test
    void getPostByIdTest() {
        postRepository.deleteAll();
        List<Post> posts = new ArrayList<>();
        Post targetPost = new Post("get this post","body");
        postRepository.save(targetPost);
        postRepository.save(new Post("post2","body"));
        postRepository.save(new Post("post3","body"));
        postRepository.findAll().forEach(posts::add);
        Post getThisPost = new Post();
        for(Post post : posts) {
            if(post.getTitle().equals("get this post")) {
                getThisPost = post;
                break;
            }
        }
        targetPost.setId(getThisPost.getId());
        assertThat(postService.getById(getThisPost.getId()).isPresent()).isTrue();
        assertThat(postService.getById(getThisPost.getId()).get()).isEqualTo(targetPost);
    }

    @Test
    void deletePostTest() {
        postRepository.deleteAll();
        Post targetPost = new Post("Delete this","body");
        Post post = new Post("Don't delete this","body");
        postRepository.save(targetPost);
        postRepository.save(post);
        List<Post> posts = postService.getAll();
        for(Post post1 : posts) {
            if(post1.getTitle().equals("Delete this")) {
                targetPost = post1;
                break;
            }
        }
        postService.deleteById(targetPost.getId());
        List<Post> result = postService.getAll();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Don't delete this");
    }

    @Test
    void deleteAllPostTest() {
        postRepository.deleteAll();
        for(int i=0; i < 5; i++) {
            postRepository.save(new Post());
        }
        postService.deleteAll();
        assertThat(postService.getAll().size()).isEqualTo(0);
    }

//    @Test
//    void getAllUsersTest() {
//
//    }
}
