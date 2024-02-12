package com.example.book.Controller;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.example.book.Model.CustomUserDetails;
import com.example.book.Model.PasswordChangeDto;
import com.example.book.Model.User;
import com.example.book.Model.UserDto;
import com.example.book.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

	private final DataLakeServiceClient dataLakeServiceClient;
	private final UserService userService;
	private final String fileSystemName;
	private final String accountName;
	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	private final RedisTemplate<String, Object> redisTemplate; // Asegúrate de que es de este tipo


    @Autowired
    public UserController(DataLakeServiceClient dataLakeServiceClient,
                          RedisTemplate<String, Object> redisTemplate,
                          UserService userService,
                          @Value("${azure.storage.file-system-name}") String fileSystemName,
                          @Value("${azure.storage.account-name}") String accountName) {
	    this.dataLakeServiceClient = dataLakeServiceClient;
	    this.userService = userService;
	    this.fileSystemName = fileSystemName;
	    this.accountName = accountName;
	    this.redisTemplate = redisTemplate;
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

		// Intenta obtener los detalles del usuario desde la caché de Redis
		Map<String, String> userInfo = (Map<String, String>) redisTemplate.opsForValue().get("user:" + userId);
		if (userInfo != null) {
			log.info("Retrieved user with ID {} from cache", userId);
			return ResponseEntity.ok(userInfo);
		}

		User userFound = userService.findUserById(userId);
		if (userFound != null) {
			userInfo = new HashMap<>();
			userInfo.put("username", userFound.getUsername());
			userInfo.put("PhotoUrl", userFound.getPhotoUrl());
			userInfo.put("Email", userFound.getEmail());

			// Almacena los detalles del usuario en la caché de Redis
			redisTemplate.opsForValue().set("user:" + userId, userInfo, 1, TimeUnit.HOURS);
			log.info("User with ID {} cached", userId);

			return ResponseEntity.ok(userInfo);
		} else {
			log.warn("User with ID {} not found", userId);
			return ResponseEntity.notFound().build();
		}
	}


	@PutMapping("/updatePassword/{userId}")
	public ResponseEntity<?> updatePassword(@PathVariable String userId, @RequestBody PasswordChangeDto passwordChangeDto) {
		log.info("Updating password for user with ID {}", userId);
		try {
			userService.updatePassword(userId, passwordChangeDto.getOldPassword(), passwordChangeDto.getNewPassword());
			return ResponseEntity.ok().build();
		} catch (UserNotFoundException e) {
			log.error("User not found for ID {}: {}", userId, e.getMessage());
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			log.error("Invalid old password for user with ID {}: {}", userId, e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}


	@PutMapping("/update/{userId}")
	public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User userDetails) {
		log.info("Updating user with ID {}", userId);
		try {
			User updatedUser = userService.updateUser(userId, userDetails);

			// Invalida la caché del usuario específico
			redisTemplate.delete("user:" + userId);
			log.info("Cache invalidated for user with ID {}", userId);

			// Recupera todas las claves de caché relacionadas con los amigos de este usuario
			String userCacheKeysSet = "friendsOf:" + userId;
			Set<Object> friendCacheKeys = redisTemplate.opsForSet().members(userCacheKeysSet);
			if (friendCacheKeys != null && !friendCacheKeys.isEmpty()) {
				// Convertir el conjunto de Object a un conjunto de String
				Set<String> keysToDelete = friendCacheKeys.stream()
						.map(Object::toString)
						.collect(Collectors.toSet());

				// Eliminar cada clave de caché de amigos individualmente
				keysToDelete.forEach(key -> {
					redisTemplate.delete("friendsDto:" + key);
					log.info("Cache invalidated for friend with ID {}", key);
				});
			}

			// Opcionalmente, limpia el conjunto 'friendsOf' para este usuario
			redisTemplate.delete(userCacheKeysSet);
			log.info("Cleared 'friendsOf' set for user with ID {}", userId);

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
	            DataLakeDirectoryClient directoryClient = fileSystemClient.getDirectoryClient("book");
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
	        DataLakeDirectoryClient directoryClient = fileSystemClient.getDirectoryClient("book");
            DataLakeFileClient fileClient = directoryClient.createFile(filename);

            try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
                fileClient.append(inputStream, 0, file.getSize());
                fileClient.flush(file.getSize());
            }

	        String fileUrl = String.format("https://%s.blob.core.windows.net/%s/book/%s", accountName, fileSystemName, filename);
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
	public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String username) {
		if (username == null || username.trim().isEmpty()) {
			log.info("Username parameter is empty or null");
			return ResponseEntity.badRequest().body(Collections.emptyList());
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentUsername = ((CustomUserDetails) authentication.getPrincipal()).getUsername();

		log.info("Searching for users with username: {}, excluding current user ID: {}", username, currentUsername);
		try {
			List<UserDto> userDtos = userService.searchUsersByUsername(username, currentUsername).stream()
					.map(user -> new UserDto(user.getId(), user.getUsername(), user.getPhotoUrl()))
					.collect(Collectors.toList());
			return ResponseEntity.ok(userDtos);
		} catch (DataAccessException e) {
			log.error("Database error searching for users with username {}: {}", username, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		} catch (Exception e) {
			log.error("Error searching for users with username {}: {}", username, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
}


