package com.example.book.Controller;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.example.book.Model.User;
import com.example.book.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {


    @Autowired
    private DataLakeServiceClient dataLakeServiceClient;

    @Value("${azure.storage.file-system-name}")
    private String fileSystemName;

    @Value("${azure.storage.account-name}")
    private String accountName;

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

            // Obtener el nombre del archivo existente
            String existingPhotoUrl = user.getPhotoUrl();
            String existingFilename = null;
            if (existingPhotoUrl != null && !existingPhotoUrl.isEmpty()) {
                existingFilename = existingPhotoUrl.substring(existingPhotoUrl.lastIndexOf('/') + 1);
            }

            // Eliminar la foto existente si está presente
            DataLakeFileSystemClient fileSystemClient = null;
            if (existingFilename != null) {
                fileSystemClient = dataLakeServiceClient.getFileSystemClient(fileSystemName);
                DataLakeDirectoryClient directoryClient = fileSystemClient.getDirectoryClient("user-photos");
                if (directoryClient.exists()) {
                    DataLakeFileClient fileClientToDelete = directoryClient.getFileClient(existingFilename);
                    if (fileClientToDelete.exists()) {
                        fileClientToDelete.delete();
                    }
                }
            }

            // Subir la nueva imagen
            String filename = userId + "_" + file.getOriginalFilename();
            DataLakeDirectoryClient directoryClient = fileSystemClient.getDirectoryClient("user-photos");
            DataLakeFileClient fileClient = directoryClient.createFile(filename);

            try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
                fileClient.append(inputStream, 0, file.getSize());
                fileClient.flush(file.getSize());
            }

            String fileUrl = String.format("https://%s.blob.core.windows.net/%s/user-photos/%s", accountName, fileSystemName, filename);
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

