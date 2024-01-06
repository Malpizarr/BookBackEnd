package org.example.bookfriendship.Model;

import java.time.LocalDateTime;

public class FriendshipDto {

    private String requesterUsername;
    private String friendUsername;
    private String status;
    private LocalDateTime createdAt;

    public FriendshipDto(String requesterUsername, String friendUsername, String status, LocalDateTime createdAt) {
        this.requesterUsername = requesterUsername;
        this.friendUsername = friendUsername;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters y Setters
    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
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

    // Método toString (opcional) para representar el objeto como String
    @Override
    public String toString() {
        return "FriendshipDto{" +
                "requesterUsername='" + requesterUsername + '\'' +
                ", friendUsername='" + friendUsername + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


