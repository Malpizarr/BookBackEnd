package org.example.bookfriendship.Model;

import java.time.LocalDateTime;

public class FriendshipDto {

    private String id;
    private String friendUsername;
    private String status;
    private LocalDateTime createdAt;

    public FriendshipDto(String id, String friendUsername, String status, LocalDateTime createdAt) {
        this.id = id;
        this.friendUsername = friendUsername;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters y Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFriendUsername() {
        return friendUsername;
    }

    public void setFriendUsername(String friendUsername) {
        this.friendUsername = friendUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FriendshipDto{" +
                "id='" + id + '\'' +
                ", friendUsername='" + friendUsername + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
