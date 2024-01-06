package com.example.book.Controller;

import com.example.book.Model.User;
import com.example.book.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public User createUser(@RequestBody User newUser) {
        return userService.createUser(newUser);
    }


    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, String>> getUserById(@PathVariable String userId) {
        User user = userService.findUserById(userId);
        if (user != null) {
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("username", user.getUsername());
            return ResponseEntity.ok(userInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(userId, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}

