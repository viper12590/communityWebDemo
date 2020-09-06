package CommunityWebDemo.controller;

import CommunityWebDemo.security.IpHandler;
import CommunityWebDemo.entity.Comment;
import CommunityWebDemo.entity.Post;
import CommunityWebDemo.entity.Thread;
import CommunityWebDemo.entity.User;
import CommunityWebDemo.service.CommentService;
import CommunityWebDemo.service.PostService;
import CommunityWebDemo.service.ThreadService;
import CommunityWebDemo.service.UserService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
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
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    VoteController voteController;
    @Autowired
    BookmarkController bookmarkController;

    IpHandler ipHandler = new IpHandler();



    @GetMapping("/{threadUrl}/posts/{id}")
    public String showPostById(@PathVariable String threadUrl, @PathVariable Long id, Model model, HttpServletRequest request) throws ResponseStatusException, JSONException {
        Optional<Thread> optionalThread = threadService.getByUrl(threadUrl);
        Optional<Post> optionalPost = postService.getById(id);
        if(optionalThread.isPresent() && optionalPost.isPresent()) {
            Post post = optionalPost.get();
            List<Comment> allComments = commentService.getCommentsOfPost(post);
            List<Comment> comments = new ArrayList<>();
            for(Comment comment : allComments) {
                if(comment.getParentComment() == null) {
                    comments.add(comment);
                }
            }
            Parser parser = Parser.builder().build();
            HtmlRenderer htmlRenderer = HtmlRenderer.builder().escapeHtml(true).softbreak("<br>").build();
            Node node = parser.parse(post.getBody());
            post.setBody(htmlRenderer.render(node));
            model.addAttribute("thread",optionalThread.get());
            model.addAttribute("post",post);
            model.addAttribute("comments",comments);
            model.addAttribute("allComments",allComments);
            //Check if current user is registered or anonymous
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                User currentUser = (User) auth.getPrincipal();
                model.addAttribute("currentUser",currentUser);
                model.addAttribute("bookmarked",bookmarkController.bookmark(threadUrl,id,"check"));
            }
            else {
                model.addAttribute("currentUser",null);
            }

            if(voteController.checkVoteBefore(threadUrl,id,true,request)) {
                model.addAttribute("upVoted",true);
                model.addAttribute("downVoted",false);
            }
            else if(voteController.checkVoteBefore(threadUrl,id,false,request)) {
                model.addAttribute("upVoted",false);
                model.addAttribute("downVoted",true);
            }
            else {
                model.addAttribute("upVoted",false);
                model.addAttribute("downVoted",false);
            }

        }
        else throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Page not found");
        postService.incrementViewOfPostById(id);
        return "post";
    }

    @GetMapping("/{threadUrl}/new_post")
    public String newPost(@PathVariable String threadUrl, Model model) throws ResponseStatusException {
        Optional<Thread> optionalThread = threadService.getByUrl(threadUrl);
        if(optionalThread.isPresent()) {
            model.addAttribute("thread",optionalThread.get());
            return "newPost";
        }
        else throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Page not found");

    }

    @PostMapping("/{threadUrl}/new_post")
    public RedirectView saveNewPost(@PathVariable String threadUrl, Post newPost, HttpServletRequest request) throws ResponseStatusException {
        Optional<Thread> optionalThread = threadService.getByUrl(threadUrl);
        if(optionalThread.isPresent()) {
            newPost.setThread(optionalThread.get());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            //Post author Anonymous or Registered
            if(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                newPost.setIp(ipHandler.trimIpAddress(request.getRemoteAddr()));
                newPost.setPassword(passwordEncoder.encode(newPost.getPassword()));
            }
            else {
                User authUser = (User) auth.getPrincipal();
                newPost.setUser(authUser);
            }
        }
        else throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Page not found");

        postService.add(newPost);
        return new RedirectView("/{threadUrl}/posts/"+newPost.getId());
    }

    @PostMapping("/{threadUrl}/posts/{id}/delete")
    public RedirectView delete(@PathVariable String threadUrl, @PathVariable Long id, String password, RedirectAttributes redirectAttr) throws ResponseStatusException{
        Optional<Thread> optionalThread = threadService.getByUrl(threadUrl);
        Optional<Post> optionalPost = postService.getById(id);
        //The thread is present, the post is present and the thread of the post is same
        if(optionalThread.isPresent() && optionalPost.isPresent() && optionalPost.get().getThread().equals(optionalThread.get())) {
            Post post = optionalPost.get();
            //Owner of the post Anonymous or Registered?
            if(post.getUser() == null) {
                //password match?
                if(passwordEncoder.matches(password, post.getPassword())) {
                    List<Comment> comments = commentService.getCommentsOfPost(optionalPost.get());
                    commentService.deleteAll(comments);
                    post.setIp(null);
                    post.setPassword(null);
                    post.setThread(null);
                    post.setActive(false);
                    postService.add(post);
                    redirectAttr.addFlashAttribute("successMessage","The post is deleted");
                    return new RedirectView("/{threadUrl}/");
                }
                //wrong password
                else {
                    redirectAttr.addFlashAttribute("failMessage","Wrong password");
                    return new RedirectView("/{threadUrl}/posts/{id}");
                }
            }
            //Registered user
            else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                //logged in?
                if(!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                    User authUser = (User) auth.getPrincipal();
                    //Is current user the owner of the post?
                    if(post.getUser().equals(authUser)) {
                        List<Comment> comments = commentService.getCommentsOfPost(optionalPost.get());
                        commentService.deleteAll(comments);
                        post.setUser(null);
                        post.setThread(null);
                        post.setActive(false);
                        postService.add(post);
                        redirectAttr.addFlashAttribute("successMessage","The post is deleted");
                        return new RedirectView("/{threadUrl}/");
                    }
                    else {
                        redirectAttr.addFlashAttribute("failMessage","Access Denied");
                        return new RedirectView("/{threadUrl}/posts/{id}");
                    }
                }
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request url");
    }

    @GetMapping("/{threadUrl}/posts/{id}/edit")
    public String updatePost(@PathVariable String threadUrl, @PathVariable Long id, Model model, RedirectAttributes redirectAttr) throws ResponseStatusException{
        Optional<Thread> optionalThread = threadService.getByUrl(threadUrl);
        Optional<Post> optionalPost = postService.getById(id);
        //The thread is present, the post is present
        if(optionalThread.isPresent() && optionalPost.isPresent()) {
            Post post = optionalPost.get();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            //is this post owned by registered user?
            if(optionalPost.get().getUser() != null) {
                //logged in?
                if(!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                    User authUser = (User) auth.getPrincipal();
                    //current user is the owner of the post
                    if(optionalPost.get().getUser().equals(authUser)) {
                        model.addAttribute("thread",optionalThread.get());
                        model.addAttribute("post", post);
                        return "updatePost";
                    }
                    //current user is not the owner of the post
                    else {
                        redirectAttr.addFlashAttribute("failMessage","Access denied");
                        return "redirect:/{threadUrl}/posts/{id}";
                    }
                }
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");
            }
            //This post was written by anonymous user
            else {
                model.addAttribute("thread",optionalThread.get());
                model.addAttribute("post", post);
                return "updatePost";
            }
        }
        //thread or post is not present, throw exception
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Page not found");
    }

    @PostMapping("/{threadUrl}/posts/{id}/edit")
    public RedirectView saveUpdatedPost(@PathVariable String threadUrl,@PathVariable Long id, Post post,RedirectAttributes redirectAttr) throws ResponseStatusException {
        Optional<Thread> optionalThread = threadService.getByUrl(threadUrl);
        Optional<Post> optionalPost = postService.getById(id);
        //The thread and the post are present
        if(optionalThread.isPresent() && optionalPost.isPresent()) {
            //apply changes
            Post targetPost = optionalPost.get();
            targetPost.setTitle(post.getTitle());
            targetPost.setBody(post.getBody());
            //the owner of the post registered or anonymous?
            if(targetPost.getUser() != null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                //logged in?
                if(!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                    User authUser = (User) auth.getPrincipal();
                    //current user is the owner of the post
                    if(targetPost.getUser().equals(authUser)) {
                        postService.add(targetPost);
                    }
                    //wrong user
                    else {
                        redirectAttr.addFlashAttribute("failMessage","Access denied");
                    }
                }
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");
            }
            //owner of the post is anonymous
            else {
                //check password
                if(passwordEncoder.matches(post.getPassword(),targetPost.getPassword())) {
                    postService.add(targetPost);
                }
                //wrong password
                else {
                    redirectAttr.addFlashAttribute("failMessage","Wrong password");
                    //redirectAttr.addFlashAttribute("post",post);
                    return new RedirectView("/{threadUrl}/posts/{id}/edit");
                }
            }

            return new RedirectView("/{threadUrl}/posts/{id}");
        }
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request url");
    }

    @GetMapping("/posts/{id}")
    public RedirectView redirectPostToThread(@PathVariable Long id) throws ResponseStatusException{
        Optional<Post> optionalPost = postService.getById(id);
        if(optionalPost.isPresent()) {
            Post post = optionalPost.get();
            Optional<Thread> optionalThread = threadService.getByUrl(post.getThread().getUrl());
            if(optionalThread.isPresent()) {
                String threadUrl = optionalThread.get().getUrl();
                return new RedirectView("/"+threadUrl+"/posts/{id}");
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Page not found");
    }

}
