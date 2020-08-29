package CommunityWebDemo.entity;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
public class Comment {

    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    private Post post;
    @ManyToOne
    private User user;

    private String ip;
    private String password;
    private String message;

    private boolean active = true;

    @ManyToOne
    private Comment parentComment;
    @OneToMany(mappedBy = "parentComment",fetch = FetchType.EAGER)
    private List<Comment> subComments;


    public Comment() {

    }

    public Comment(Post post, User user, String message) {
        this.post = post;
        this.user = user;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Comment getParentComment() {
        return parentComment;
    }

    public void setParentComment(Comment parentComment) {
        this.parentComment = parentComment;
    }

    public List<Comment> getSubComments() {
        return subComments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return active == comment.active &&
                Objects.equals(id, comment.id) &&
                Objects.equals(post, comment.post) &&
                Objects.equals(user, comment.user) &&
                Objects.equals(ip, comment.ip) &&
                Objects.equals(password, comment.password) &&
                Objects.equals(message, comment.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, post, user, ip, password, message, active);
    }
}