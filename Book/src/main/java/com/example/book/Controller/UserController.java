package com.example.book.Controller;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.example.book.Model.User;
import com.example.book.Model.UserDto;
import com.example.book.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

	private final DataLakeServiceClient dataLakeServiceClient;
	private final UserService userService;
	private final String fileSystemName;
	private final String accountName;
	private static final Logger log = LoggerFactory.getLogger(UserController.class);


    @Autowired
    public UserController(DataLakeServiceClient dataLakeServiceClient,
                          UserService userService,
                          @Value("${azure.storage.file-system-name}") String fileSystemName,
                          @Value("${azure.storage.account-name}") String accountName) {
	    this.dataLakeServiceClient = dataLakeServiceClient;
	    this.userService = userService;
	    this.fileSystemName = fileSystemName;
	    this.accountName = accountName;
    }

	public static class UserNotFoundException extends RuntimeException {
		public UserNotFoundException(String message) {
			super(message);
		}
	}



    @PostMapping("/create")
    public User createUser(@RequestBody User newUser) {
	    log.info("Creating new user");
        return userService.createUser(newUser);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, String>> getUserById(@PathVariable String userId) {
	    log.info("Fetching user with ID {}", userId);
        User user = userService.findUserById(userId);
        if (user != null) {
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("username", user.getUsername());
            userInfo.put("PhotoUrl", user.getPhotoUrl());
            return ResponseEntity.ok(userInfo);
        } else {
	        log.warn("User with ID {} not found", userId);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User userDetails) {
	    log.info("Updating user with ID {}", userId);
        try {
            User updatedUser = userService.updateUser(userId, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
	        log.error("User not found for ID {}: {}", userId, e.getMessage());
	        return ResponseEntity.notFound().build();
        } catch (Exception e) {
	        log.error("Error updating user with ID {}: {}", userId, e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/uploadPhoto/{userId}")
    public ResponseEntity<?> uploadUserPhoto(@PathVariable String userId, @RequestParam("file") MultipartFile file) {
	    log.info("Uploading photo for user with ID {}", userId);
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
	        DataLakeFileSystemClient fileSystemClient = dataLakeServiceClient.getFileSystemClient(fileSystemName);
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
	        assert fileSystemClient != null;
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
	        log.error("Error uploading photo for user ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir la imagen: " + e.getMessage());
        }
    }

	@GetMapping("/search")
	public ResponseEntity<?> searchUsers(@RequestParam String username) {
		log.info("Searching for users with username {}", username);
		try {
			List<User> users = userService.searchUsersByUsername(username);
			// Convierte la lista de User a una lista de UserDto
			List<UserDto> userDtos = users.stream()
					.map(user -> new UserDto(user.getId(), user.getUsername(), user.getPhotoUrl()))
					.collect(Collectors.toList());
			return ResponseEntity.ok(userDtos);
		} catch (Exception e) {
			log.error("Error searching for users with username {}: {}", username, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}


}

