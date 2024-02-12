package org.example.bookfriendship.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.example.bookfriendship.Model.Friendship;
import org.example.bookfriendship.Model.FriendshipDto;
import org.example.bookfriendship.Repository.FriendshipRepository;
import org.example.bookfriendship.Service.FriendshipService;
import org.example.bookfriendship.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private final RedisTemplate<String, Object> redisTemplate; // Asegúrate de que es de este tipo

    private static final Logger log = LoggerFactory.getLogger(FriendshipController.class);

    @Autowired
    private ObjectMapper objectMapper;

    public FriendshipController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Friendship>> getFriendships(@PathVariable String userId) {
        List<Friendship> friends = (List<Friendship>) redisTemplate.opsForValue().get("friendsDto:" + userId);

        if (friends != null && !friends.isEmpty()) {
            log.info("Retrieved friendships for user with ID {} from cache", userId);
            return ResponseEntity.ok(friends);
        } else {
            List<Friendship> friendships = friendshipRepository.findByRequesterId(userId);

            if (friendships != null && !friendships.isEmpty()) {
                redisTemplate.opsForValue().set("friendsDto:" + userId, friendships, 1, TimeUnit.HOURS);
            }

            return ResponseEntity.ok(friendships);
        }
    }

    @PutMapping("/accept")
    public ResponseEntity<?> acceptFriendship(@RequestParam String friendshipId) {
        System.out.println(friendshipId);
        try {
            List<String> affectedUserIds = friendshipService.acceptFriendship(friendshipId);

            for (String userId : affectedUserIds) {
                String cacheKey = "friendsDto:" + userId;
                redisTemplate.delete(cacheKey);
                log.info("Cache invalidated for user with ID {}", userId);
            }

            for (String userId : affectedUserIds) {
                String cacheKey = "pendingFriendships:" + userId;
                redisTemplate.delete(cacheKey);
                log.info("Cache invalidated for user with ID {}", userId);
            }
            return ResponseEntity.ok(200);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al aceptar la amistad: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable String userId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity("http://localhost:8081/users/" + userId, Map.class);
            Map<String, Object> userDetails = response.getBody();
            return ResponseEntity.ok(userDetails);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener detalles del usuario");
        }
    }


    @PostMapping("/createfriendship")
    public ResponseEntity<?> createFriendship(@RequestBody Friendship newFriendship) {
        try {
            Friendship friendship = friendshipService.createFriendship(newFriendship.getRequesterId(), newFriendship.getFriendId());
            redisTemplate.delete("friendsDto:" + friendship.getRequesterId());
            redisTemplate.delete("friendsDto:" + friendship.getFriendId());
            redisTemplate.delete("pendingFriendships:" + friendship.getFriendId());
            redisTemplate.delete("pendingFriendships:" + friendship.getRequesterId());
            log.info("Cache invalidated for user with ID {}", friendship.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(friendship);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/deletefriendship")
    public ResponseEntity<?> deleteFriendship(@RequestParam String friendshipId) {
        try {
            // Suponiendo que deleteFriendship ahora devuelve una lista de userId afectados
            List<String> affectedUserIds = friendshipService.deleteFriendship(friendshipId);

            // Invalidar caché para ambos usuarios
            for (String userId : affectedUserIds) {
                String cacheKey = "friendsDto:" + userId;
                redisTemplate.delete(cacheKey);
                log.info("Cache invalidated for user with ID {}", userId);
            }

            for (String userId : affectedUserIds) {
                String cacheKey = "pendingFriendships:" + userId;
                redisTemplate.delete(cacheKey);
                log.info("Cache invalidated for user with ID {}", userId);
            }

            return ResponseEntity.ok("Amistad eliminada");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al eliminar la amistad: " + e.getMessage());
        }
    }


    @GetMapping("/areFriends/{userId1}/{userId2}")
    public ResponseEntity<Boolean> areFriends(@PathVariable String userId1, @PathVariable String userId2) {
        try {
            boolean areFriends = friendshipService.areFriends(userId1, userId2);
            return ResponseEntity.ok(areFriends);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(false);
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPending(HttpServletRequest request) {
        try {
            String userId = jwtTokenUtil.getUserIdFromToken(jwtTokenUtil.obtenerJwtDeLaSolicitud(request));

            String cacheKey = "pendingFriendships:" + userId;

            List<FriendshipDto> cachedFriendships = (List<FriendshipDto>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedFriendships != null) {
                return ResponseEntity.ok(cachedFriendships);
            } else {
                List<FriendshipDto> friendshipsWithUsernames = friendshipService.getPendingFriendshipDetailsWithUsernames(userId);

                redisTemplate.opsForValue().set(cacheKey, friendshipsWithUsernames, Duration.ofHours(1)); // Ajusta la duración según tus necesidades

                return ResponseEntity.ok(friendshipsWithUsernames);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener amistades pendientes: " + e.getMessage());
        }
    }


    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(HttpServletRequest request) {
        try {
            String userId = jwtTokenUtil.getUserIdFromToken(jwtTokenUtil.obtenerJwtDeLaSolicitud(request));
            String cacheKey = "friendsDto:" + userId; // Clave de caché para los amigos del usuario

            // Intenta recuperar la lista de amigos desde la caché
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            List<FriendshipDto> cachedFriends;

            if (cachedData instanceof List<?>) {
                cachedFriends = ((List<?>) cachedData).stream()
                        .filter(LinkedHashMap.class::isInstance)
                        .map(obj -> objectMapper.convertValue(obj, FriendshipDto.class))
                        .collect(Collectors.toList());

                if (!cachedFriends.isEmpty()) {
                    log.info("Retrieved friends for user with ID {} from cache", userId);
                    // Agrega los IDs de amigos al conjunto de amigos de cada amigo
                    cachedFriends.forEach(friendship ->
                            redisTemplate.opsForSet().add("friendsOf:" + friendship.getFriendId(), userId));
                    return ResponseEntity.ok(cachedFriends);
                }
            }

            List<FriendshipDto> friendshipsWithUsernames = friendshipService.getFriendshipDetailsWithUsernames(userId);
            if (!friendshipsWithUsernames.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, friendshipsWithUsernames, 1, TimeUnit.HOURS);
                // Por cada amigo, agrega este userId al conjunto 'friendsOf:' del amigo
                friendshipsWithUsernames.forEach(friendship ->
                        redisTemplate.opsForSet().add("friendsOf:" + friendship.getFriendId(), userId));
            }

            return ResponseEntity.ok(friendshipsWithUsernames);
        } catch (JwtException e) {
            log.error("JWT parsing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token");
        } catch (Exception e) {
            log.error("Error retrieving friends list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener lista de amigos: " + e.getMessage());
        }
    }


}
