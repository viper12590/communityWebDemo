package CommunityWebDemo.controller;

import CommunityWebDemo.security.IpHandler;
import CommunityWebDemo.entity.Comment;
import CommunityWebDemo.entity.Post;
import CommunityWebDemo.entity.User;
import CommunityWebDemo.service.CommentService;
import CommunityWebDemo.service.PostService;
import CommunityWebDemo.service.ThreadService;
import CommunityWebDemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Controller
public class CommentController {

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

    IpHandler ipHandler = new IpHandler();

    @PostMapping("/posts/{postId}/new_comment")
    public RedirectView addComment(@PathVariable Long postId, Comment comment, HttpServletRequest request) throws ResponseStatusException {
        Optional<Post> optionalPost = postService.getById(postId);
        if(optionalPost.isPresent()) {
            Post post = optionalPost.get();
            comment.setPost(post);

            //Anonymous user or registered user?
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                comment.setIp(ipHandler.trimIpAddress(request.getRemoteAddr()));
                comment.setPassword(passwordEncoder.encode(comment.getPassword()));
            }
            else {
                User authUser = (User) auth.getPrincipal();
                Optional<User> optionalUser = userService.getById(authUser.getId());
                if(optionalUser.isPresent()) {
                    comment.setUser(optionalUser.get());
                }
                else return new RedirectView("/logout");

            }
            commentService.add(comment);
            if(post.getThread() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No thread for the post");
            }
            return new RedirectView("/" + post.getThread().getUrl() + "/posts/{postId}");
        }
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request url");
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/delete")
    public RedirectView deleteComment(@PathVariable Long postId, @PathVariable Long commentId, String password, RedirectAttributes redirectAttr) throws ResponseStatusException{
        Optional<Post> optionalPost = postService.getById(postId);
        Optional<Comment> optionalComment = commentService.getById(commentId);
        //Post is present, comment is present, thread is not null
        if(optionalPost.isPresent() && optionalComment.isPresent() && optionalPost.get().getThread() != null) {
            Comment comment = optionalComment.get();
            //Owner of the comment Anonymous user or registered user?
            if(comment.getUser() == null) {
                //Comment password is correct
                if(passwordEncoder.matches(password, comment.getPassword())) {
                    emptyComment(commentService,comment);
                    redirectAttr.addFlashAttribute("successMessage","The Comment is deleted");
                }
                else {
                    redirectAttr.addFlashAttribute("failMessage","Password is incorrect");
                }
            }
            else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = (User) auth.getPrincipal();
                //Current logged in user is the owner of the comment
                if(comment.getUser().getId().equals(user.getId())) {
                    emptyComment(commentService,comment);
                    redirectAttr.addFlashAttribute("successMessage","The Comment is deleted");
                }
                else {
                    redirectAttr.addFlashAttribute("failMessage","Access denied");
                }
            }
            return new RedirectView("/" + optionalPost.get().getThread().getUrl() + "/posts/{postId}");
        }
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request url");
    }

    @GetMapping("/posts/{postId}/comments/{commentId}/reply")
    public String newReply(@PathVariable Long postId, @PathVariable Long commentId, Model model) throws ResponseStatusException{
        Optional<Post> optionalPost = postService.getById(postId);
        Optional<Comment> optionalComment = commentService.getById(commentId);
        //post and comment exist and the comment is from the post
        if(optionalPost.isPresent() && optionalComment.isPresent() && optionalComment.get().getPost().equals(optionalPost.get())) {
            model.addAttribute("post",optionalPost.get());
            model.addAttribute("parentComment",optionalComment.get());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                model.addAttribute("currentUser", auth.getPrincipal());
            }
            return "reply";
        }
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bad request url");
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/reply")
    public RedirectView saveNewReply(@PathVariable Long postId, @PathVariable Long commentId, Comment reply, HttpServletRequest request) {
        Optional<Post> optionalPost = postService.getById(postId);
        Optional<Comment> optionalComment = commentService.getById(commentId);
        //post and comment exist and the comment is from the post
        if(optionalPost.isPresent() && optionalComment.isPresent() && optionalComment.get().getPost().equals(optionalPost.get())) {
            //Anonymous user or registered user?
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
                reply.setIp(ipHandler.trimIpAddress(request.getRemoteAddr()));
                reply.setPassword(passwordEncoder.encode(reply.getPassword()));
            }
            else {
                User authUser = (User) auth.getPrincipal();
                Optional<User> optionalUser = userService.getById(authUser.getId());
                if(optionalUser.isPresent()) {
                    reply.setUser(optionalUser.get());
                }
                else return new RedirectView("/logout");
            }
            reply.setPost(optionalPost.get());
            reply.setParentComment(optionalComment.get());
            commentService.add(reply);
            return new RedirectView("/posts/{postId}");
        }
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bad request url");
    }

    public void emptyComment(CommentService commentService,Comment comment) {
        comment.setMessage(null);
        comment.setUser(null);
        comment.setActive(false);
        commentService.add(comment);
    }

}
