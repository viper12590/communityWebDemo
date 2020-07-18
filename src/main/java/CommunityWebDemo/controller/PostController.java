package CommunityWebDemo.controller;

import CommunityWebDemo.entity.Comment;
import CommunityWebDemo.entity.Post;
import CommunityWebDemo.entity.Thread;
import CommunityWebDemo.entity.User;
import CommunityWebDemo.service.CommentService;
import CommunityWebDemo.service.PostService;
import CommunityWebDemo.service.ThreadService;
import CommunityWebDemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Optional;

@Controller
public class PostController {

    @Autowired
    PostService postService;
    @Autowired
    UserService userService;
    @Autowired
    CommentService commentService;
    @Autowired
    ThreadService threadService;

    User testUser = new User("tester");

    @GetMapping("/posts")
    public String showAllPosts(Model model) {
        List<Post> posts = postService.getAll();
        model.addAttribute("posts",posts);
        return "postList";
    }

    @GetMapping("/{threadInitial}/posts")
    public String showAllPostsOfThread(@PathVariable String threadInitial,Model model) throws Exception {
        Optional<Thread> optionalThread = threadService.getByInitial(threadInitial);
        List<Post> posts;
        String threadName;
        if(optionalThread.isPresent()) {
            posts = postService.getPostsOfThread(optionalThread.get());
            model.addAttribute("thread",optionalThread.get());
            model.addAttribute("posts",posts);
            return "postList";
        }
        else throw new Exception();


    }

    @GetMapping("/posts/{id}")
    public String showPostById(@PathVariable Long id, Model model) throws Exception {
        Optional<Post> optionalPost = postService.getById(id);

        List<Comment> comments;

        Post post;
        if(optionalPost.isPresent()) {
            post = optionalPost.get();
            comments = commentService.getCommentsOfPost(post);
        }
        else throw new Exception();

        model.addAttribute("post",post);
        model.addAttribute("comments",comments);
        return "post";
    }

    @GetMapping("/{threadInitial}/posts/{id}")
    public String showPostById(@PathVariable String threadInitial, @PathVariable Long id, Model model) throws Exception {
        Optional<Thread> optionalThread = threadService.getByInitial(threadInitial);
        if(optionalThread.isPresent()) {
            Optional<Post> optionalPost = postService.getById(id);

            List<Comment> comments;

            Post post;
            if(optionalPost.isPresent()) {
                post = optionalPost.get();
                comments = commentService.getCommentsOfPost(post);
            }
            else throw new Exception();

            model.addAttribute("post",post);
            model.addAttribute("comments",comments);
        }
        else throw new Exception();

        return "post";
    }


    @GetMapping("/posts/new_post")
    public String newPost() {
        return "newPost";
    }

    @GetMapping("/{threadInitial}/new_post")
    public String newPost(@PathVariable String threadInitial, Model model) throws Exception {
        Optional<Thread> optionalThread = threadService.getByInitial(threadInitial);
        if(optionalThread.isPresent()) {
            model.addAttribute("thread",optionalThread.get());
            return "newPost";
        }
        else throw new Exception();

    }

    @PostMapping("/posts/new_post")
    public RedirectView saveNewPost(Post newPost) {
        //temporary
        userService.add(testUser);
        newPost.setUser(testUser);
        postService.add(newPost);
        return new RedirectView("/posts");
    }

    @PostMapping("/{threadInitial}/new_post")
    public RedirectView saveNewPost(@PathVariable String threadInitial, Post newPost) throws Exception {
        Optional<Thread> optionalThread = threadService.getByInitial(threadInitial);
        if(optionalThread.isPresent()) {
            newPost.setThread(optionalThread.get());
        }
        else throw new Exception();
        //temporary
        userService.add(testUser);
        newPost.setUser(testUser);
        postService.add(newPost);
        return new RedirectView("/{threadInitial}/posts");
    }

    @PostMapping("/posts/{id}/delete")
    public RedirectView delete(@PathVariable Long id) {
        Optional<Post> optionalPost = postService.getById(id);
        if(optionalPost.isPresent()) {
            List<Comment> comments = commentService.getCommentsOfPost(optionalPost.get());
            commentService.deleteAll(comments);
            postService.deleteById(id);
            return new RedirectView("/posts");
        }
        else return new RedirectView("/error");
    }

    @GetMapping("/posts/{id}/edit")
    public String updatePost(@PathVariable Long id, Model model) throws Exception{
        Optional<Post> optionalPost = postService.getById(id);
        Post post;
        if(optionalPost.isPresent()) {
            post = optionalPost.get();
            model.addAttribute("post", post);
            return "updatePost";
        }
        else throw new Exception();
    }

    @PostMapping("/posts/{id}/edit")
    public RedirectView saveUpdatedPost(@PathVariable Long id, Post post) {
        Optional<Post> optionalPost = postService.getById(id);
        if(optionalPost.isPresent()) {
            Post oldPost = optionalPost.get();
            post.setId(oldPost.getId());
            post.setUser(oldPost.getUser());
            postService.add(post);
            return new RedirectView("/posts/{id}");
        }
        else return new RedirectView("/error");
    }

}
