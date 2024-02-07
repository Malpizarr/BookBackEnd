package org.example.bookfriendship.Controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.example.bookfriendship.Model.Friendship;
import org.example.bookfriendship.Model.FriendshipDto;
import org.example.bookfriendship.Repository.FriendshipRepository;
import org.example.bookfriendship.Service.FriendshipService;
import org.example.bookfriendship.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/{userId}")
    public ResponseEntity<List<Friendship>> getFriendships(@PathVariable String userId) {
        List<Friendship> friendships = friendshipRepository.findByRequesterId(userId);
        return ResponseEntity.ok(friendships);
    }

    @PutMapping("/accept")
    public ResponseEntity<?> acceptFriendship(@RequestParam String friendshipId) {
        System.out.println(friendshipId);
        try {
            Friendship acceptedFriendship = friendshipService.acceptFriendship(friendshipId);
            return ResponseEntity.ok(acceptedFriendship);
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
            return ResponseEntity.status(HttpStatus.CREATED).body(friendship);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
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
            List<FriendshipDto> friendshipsWithUsernames = friendshipService.getPendingFriendshipDetailsWithUsernames(userId);
            return ResponseEntity.ok(friendshipsWithUsernames);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener amistades pendientes: " + e.getMessage());
        }
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(HttpServletRequest request) {
        try {
            String userId = jwtTokenUtil.getUserIdFromToken(jwtTokenUtil.obtenerJwtDeLaSolicitud(request));
            List<FriendshipDto> friendshipsWithUsernames = friendshipService.getFriendshipDetailsWithUsernames(userId);
            return ResponseEntity.ok(friendshipsWithUsernames);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener lista de amigos: " + e.getMessage());
        }
    }


}
