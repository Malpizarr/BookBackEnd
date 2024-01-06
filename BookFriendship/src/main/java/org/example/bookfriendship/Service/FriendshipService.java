

package org.example.bookfriendship.Service;

import org.example.bookfriendship.Model.Friendship;
import org.example.bookfriendship.Model.FriendshipDto;
import org.example.bookfriendship.Repository.FriendshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private RestTemplate restTemplate;

    public Friendship createFriendship(String requesterId, String friendId) {
        boolean friendshipExists = friendshipRepository
                .existsByRequesterIdAndFriendId(requesterId, friendId) ||
                friendshipRepository
                        .existsByRequesterIdAndFriendId(friendId, requesterId);

        if (friendshipExists) {
            throw new IllegalStateException("Ya existe una amistad o solicitud de amistad entre estos usuarios");
        }

        Friendship friendship = new Friendship();
        friendship.setRequesterId(requesterId);
        friendship.setFriendId(friendId);
        friendship.setCreatedAt(LocalDateTime.now());
        friendship.setStatus("pending");

        return friendshipRepository.save(friendship);
    }

    public Friendship acceptFriendship(String friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("No se encontró la amistad"));

        friendship.setStatus("accepted");
        return friendshipRepository.save(friendship);
    }

    public List<FriendshipDto> getFriendshipDetailsWithUsernames(String userId) {
        List<Friendship> requesterFriendships = friendshipRepository.findByRequesterIdAndStatus(userId, "accepted");
        List<Friendship> friendFriendships = friendshipRepository.findByFriendIdAndStatus(userId, "accepted");

        Set<Friendship> uniqueFriendships = new HashSet<>();
        uniqueFriendships.addAll(requesterFriendships);
        uniqueFriendships.addAll(friendFriendships);

        Set<String> userIds = uniqueFriendships.stream()
                .map(friendship -> Arrays.asList(friendship.getRequesterId(), friendship.getFriendId()))
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Map<String, String> usernameMap = fetchUsernames(userIds);

        return uniqueFriendships.stream()
                .map(friendship -> {
                    String requesterUsername = usernameMap.getOrDefault(friendship.getRequesterId(), "Unknown");
                    String friendUsername = usernameMap.getOrDefault(friendship.getFriendId(), "Unknown");

                    // Ajusta los nombres según el usuario que realiza la consulta
                    if (friendship.getRequesterId().equals(userId)) {
                        return new FriendshipDto(requesterUsername, friendUsername, friendship.getStatus(), friendship.getCreatedAt());
                    } else {
                        return new FriendshipDto(friendUsername, requesterUsername, friendship.getStatus(), friendship.getCreatedAt());
                    }
                })
                .collect(Collectors.toList());
    }

    public Map<String, String> fetchUsernames(Set<String> userIds) {
        Map<String, String> usernameMap = new HashMap<>();
        userIds.forEach(id -> {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity("http://localhost:8081/users/" + id, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                    Map<String, Object> userDetails = response.getBody();
                    String username = (String) userDetails.get("username");
                    if (username != null) {
                        usernameMap.put(id, username);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Consider using a logger here
            }
        });
        return usernameMap;
    }
}
