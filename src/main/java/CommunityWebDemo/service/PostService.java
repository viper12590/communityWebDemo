package CommunityWebDemo.service;

import CommunityWebDemo.entity.Post;
import CommunityWebDemo.entity.Tag;
import CommunityWebDemo.entity.Thread;
import CommunityWebDemo.entity.User;
import CommunityWebDemo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class PostService extends MyService<Post>{

    @Autowired
    private PostRepository postRepository;

    @Override
    public void add(Post post) {
        postRepository.save(post);
    }

    @Override
    public void addAll(Iterable<Post> posts) {
        postRepository.saveAll(posts);
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        postRepository.findAll().forEach(posts::add);
        return posts;
    }

    @Override
    public Optional<Post> getById(Long id) {
        return postRepository.findById(id);
    }

    @Override
    public boolean deleteById(Long id) {
        if(postRepository.findById(id).isPresent()) {
            postRepository.deleteById(id);
            return true;
        }
        else return false;
    }

    @Override
    public void deleteAll() {
        postRepository.deleteAll();
    }

    public void deleteAll(Iterable<Post> posts) {
        postRepository.deleteAll(posts);
    }

    public List<Post> getPostsOfUser(User user) {
        List<Post> posts = new ArrayList<>();
        postRepository.findAll().forEach(post -> {
            if (post.getUser() != null && post.getUser().equals(user)) {
                posts.add(post);
            }});

        return posts;
    }

    public List<Post> getPostsOfThread(Thread thread) {
        List<Post> posts = new ArrayList<>();
        postRepository.findAll().forEach(post -> {
            if (post.getThread() != null && post.getThread().equals(thread)) {
                posts.add(post);
            }
        });
        return posts;
    }

    public List<Post> getPostsOfTag(Tag tag) {
        List<Post> posts = new ArrayList<>();
        postRepository.findAll().forEach(post -> {
            if(post.getTag() != null && post.getTag().equals(tag)) {
                posts.add(post);
            }
        });
        return posts;
    }

}
