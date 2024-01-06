package org.example.bookfriendship.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.User;
import org.example.bookfriendship.Model.Friendship;
import org.example.bookfriendship.Model.FriendshipDto;
import org.example.bookfriendship.Repository.FriendshipRepository;
import org.example.bookfriendship.Service.FriendshipService;
import org.example.bookfriendship.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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


    @PostMapping("/{requesterId}/{friendId}")
    public ResponseEntity<?> createFriendship(@PathVariable String requesterId, @PathVariable String friendId) {
        try {
            Friendship friendship = friendshipService.createFriendship(requesterId, friendId);
            return ResponseEntity.status(HttpStatus.CREATED).body(friendship);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<FriendshipDto>> getFriends(@PathVariable String userId, HttpServletRequest request) {
        String jwtToken = jwtTokenUtil.obtenerJwtDeLaSolicitud(request);

        List<FriendshipDto> friendshipsWithUsernames = friendshipService.getFriendshipDetailsWithUsernames(userId);
        return ResponseEntity.ok(friendshipsWithUsernames);
    }


}
