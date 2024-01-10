package com.example.book.Controller;

import com.amazonaws.services.s3.AmazonS3;
import com.example.book.Model.User;
import com.example.book.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {


    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private UserService userService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

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
            userInfo.put("PhotoUrl", user.getPhotoUrl());
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

    @PostMapping("/uploadPhoto/{userId}")
    public ResponseEntity<?> uploadUserPhoto(@PathVariable String userId, @RequestParam("file") MultipartFile file) {
        try {
            User user = userService.findUserById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            String filename = userId + "_" + file.getOriginalFilename();
            s3Client.putObject(bucketName, filename, file.getInputStream(), null);

            String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + filename;

            user.setPhotoUrl(fileUrl);
            userService.updateUser(userId, user);

            Map<String, String> response = new HashMap<>();
            response.put("photoUrl", fileUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir la imagen: " + e.getMessage());
        }
    }

}

