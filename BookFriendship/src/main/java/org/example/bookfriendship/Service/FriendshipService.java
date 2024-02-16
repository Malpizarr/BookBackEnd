

package org.example.bookfriendship.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookfriendship.Controller.FriendshipController;
import org.example.bookfriendship.Model.Friendship;
import org.example.bookfriendship.Model.FriendshipDto;
import org.example.bookfriendship.Repository.FriendshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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

	@Autowired
	private final RedisTemplate<String, Object> redisTemplate; // Asegúrate de que es de este tipo

	@Autowired
	private ObjectMapper objectMapper;

	private static final Logger log = LoggerFactory.getLogger(FriendshipController.class);

	public FriendshipService(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

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

	public List<String> acceptFriendship(String friendshipId) {
		List<String> userIds = new ArrayList<>();
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("No se encontró la amistad"));

        friendship.setStatus("accepted");
		friendshipRepository.save(friendship);

		userIds.add(friendship.getRequesterId());
		userIds.add(friendship.getFriendId());

		return userIds;
    }

    public List<FriendshipDto> getFriendshipDetailsWithUsernames(String userId) {
        List<Friendship> requesterFriendships = friendshipRepository.findByRequesterIdAndStatus(userId, "accepted");
        List<Friendship> friendFriendships = friendshipRepository.findByFriendIdAndStatus(userId, "accepted");

        Set<String> userIds = Stream.concat(requesterFriendships.stream(), friendFriendships.stream())
                .map(friendship -> Arrays.asList(friendship.getRequesterId(), friendship.getFriendId()))
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Map<String, String> usernameMap = fetchUsernames(userIds);

        return Stream.concat(requesterFriendships.stream(), friendFriendships.stream())
                .map(friendship -> {
                    String actualFriendId;
                    String otherUsername;
	                String photoUrl;

                    if (friendship.getRequesterId().equals(userId)) {
                        actualFriendId = friendship.getFriendId();
                        otherUsername = usernameMap.getOrDefault(actualFriendId, "Unknown");
	                    photoUrl = usernameMap.getOrDefault(actualFriendId + "_photoUrl", null);
                    } else {
                        actualFriendId = friendship.getRequesterId();
                        otherUsername = usernameMap.getOrDefault(actualFriendId, "Unknown");
	                    photoUrl = usernameMap.getOrDefault(actualFriendId + "_photoUrl", null);
                    }

	                return new FriendshipDto(friendship.getId(), friendship.getRequesterId(), otherUsername, actualFriendId, friendship.getStatus(), friendship.getCreatedAt(), photoUrl);
                })
                .collect(Collectors.toList());
    }




    public Map<String, String> fetchUsernames(Set<String> userIds) {
        Map<String, String> usernameMap = new HashMap<>();

        userIds.forEach(id -> {
	        Map<String, String> userInfo = (Map<String, String>) redisTemplate.opsForValue().get("user:" + id);
	        System.out.println(userInfo);

	        if (userInfo != null && !userInfo.isEmpty()) {
		        if (userInfo.containsKey("username")) {
			        usernameMap.put(id, userInfo.get("username"));
		        }
		        if (userInfo.containsKey("PhotoUrl")) {
			        usernameMap.put(id + "_photoUrl", userInfo.get("PhotoUrl"));
		        }
		        if (userInfo.containsKey("Email")) {
			        usernameMap.put(id + "_email", userInfo.get("Email"));
		        }


	        } else {
		        fetchAndCacheUserInfo(id, usernameMap);
	        }
        });

	    return usernameMap;
    }

	private void fetchAndCacheUserInfo(String id, Map<String, String> usernameMap) {
		ResponseEntity<Map> response = restTemplate.getForEntity("https://bookauth-c0fd8fb7a366.herokuapp.com/users/" + id, Map.class);
		if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
			Map<String, Object> userDetails = response.getBody();
			Map<String, String> userInfoToCache = new HashMap<>();
			userInfoToCache.put("username", (String) userDetails.get("username"));
			userInfoToCache.put("PhotoUrl", (String) userDetails.get("PhotoUrl"));
			userInfoToCache.put("Email", (String) userDetails.get("Email"));


			usernameMap.put(id, userInfoToCache.get("username"));
			usernameMap.put(id + "_photoUrl", userInfoToCache.get("PhotoUrl"));

			redisTemplate.opsForValue().set("user:" + id, userInfoToCache, Duration.ofHours(1));
		} else {
			log.error("No se pudo obtener detalles del usuario del servicio externo para el ID {}: StatusCode {}", id, response.getStatusCode());
		}
	}





    public List<FriendshipDto> getPendingFriendshipDetailsWithUsernames(String userId) {
        List<Friendship> requesterFriendships = friendshipRepository.findByRequesterIdAndStatus(userId, "pending");
        List<Friendship> friendFriendships = friendshipRepository.findByFriendIdAndStatus(userId, "pending");

	    Set<String> userIds = Stream.concat(requesterFriendships.stream(), friendFriendships.stream())
			    .map(friendship -> Arrays.asList(friendship.getRequesterId(), friendship.getFriendId()))
			    .flatMap(List::stream)
			    .collect(Collectors.toSet());

	    Map<String, String> usernameMap = fetchUsernames(userIds);

	    return Stream.concat(requesterFriendships.stream(), friendFriendships.stream())
			    .map(friendship -> {
				    String actualFriendId;
				    String otherUsername;
				    String photoUrl;
				    String requesterId = friendship.getRequesterId();

				    if (friendship.getRequesterId().equals(userId)) {
					    actualFriendId = friendship.getFriendId();
					    otherUsername = usernameMap.getOrDefault(actualFriendId, "Unknown");
					    photoUrl = usernameMap.getOrDefault(actualFriendId + "_photoUrl", null);
				    } else {
					    actualFriendId = friendship.getRequesterId();
					    otherUsername = usernameMap.getOrDefault(actualFriendId, "Unknown");
					    photoUrl = usernameMap.getOrDefault(actualFriendId + "_photoUrl", null);
				    }

				    return new FriendshipDto(friendship.getId(), requesterId, otherUsername, actualFriendId, friendship.getStatus(), friendship.getCreatedAt(), photoUrl);
			    })
			    .collect(Collectors.toList());
    }

	public boolean areFriends(String userId1, String userId2) {
		List<Friendship> friendshipsAsRequester = friendshipRepository.findByRequesterIdAndFriendId(userId1, userId2);
		List<Friendship> friendshipsAsFriend = friendshipRepository.findByFriendIdAndRequesterId(userId1, userId2);

		return Stream.concat(friendshipsAsRequester.stream(), friendshipsAsFriend.stream())
				.anyMatch(f -> f.getStatus().equals("accepted"));
	}

	public List<String> deleteFriendship(String friendshipId) {
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new RuntimeException("No se encontró la amistad"));

		friendshipRepository.delete(friendship);

		return Arrays.asList(friendship.getRequesterId(), friendship.getFriendId());

	}
}


