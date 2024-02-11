package com.example.book.Service;

import com.example.book.Controller.UserController;
import com.example.book.Model.CustomUserDetails;
import com.example.book.Model.LoginResponse;
import com.example.book.Model.Role;
import com.example.book.Model.User;
import com.example.book.Repositories.RoleRepository;
import com.example.book.Repositories.UserRepository;
import com.example.book.Util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;


@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public User register(User newUser) {
        // Busca el rol por defecto
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Asigna los roles al nuevo usuario
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);
        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            throw new RuntimeException("Email already taken");
        }

        if (!EMAIL_PATTERN.matcher(newUser.getEmail()).matches()) {
            throw new RuntimeException("Invalid email format");
        }

        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        return userRepository.save(newUser);
    }


    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtTokenUtil.createToken(user);

        return new LoginResponse(accessToken);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Asegúrate de que tu clase CustomUserDetails acepte los parámetros necesarios en su constructor
        return new CustomUserDetails(user.getUsername(), user.getPassword(), new ArrayList<>(), user.getId());
    }


    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }


    public User createUser(User newUser) {
        return userRepository.save(newUser);
    }

    public User findUserById(String userId) {
        return userRepository.findById(userId)
		        .or(() -> userRepository.findByUsername(userId))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public User updateUser(String userId, User userDetails) {
        User user = userRepository.findById(userId)
		        .or(() -> userRepository.findByUsername(userId))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
	    user.setPhotoUrl(userDetails.getPhotoUrl());

        return userRepository.save(user);
    }


	public User searchUsers(String username) {

		User user = userRepository.findByUsername(username)
				.or(() -> userRepository.findByEmail(username))
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
		return user;
	}

	public List<User> searchUsersByUsername(String username, String currentUser) {
		List<User> users = userRepository.findByUsernameContaining(username);
		if (users.isEmpty()) {
			throw new RuntimeException("Usuario no encontrado");
		}

		// Usar Iterator para evitar ConcurrentModificationException
		Iterator<User> iterator = users.iterator();
		while (iterator.hasNext()) {
			User user = iterator.next();
			// Comparar ignorando mayúsculas/minúsculas si es necesario
			if (user.getUsername().equalsIgnoreCase(currentUser)) {
				iterator.remove(); // Eliminar el usuario actual de la lista
			}
		}


		return users;
	}

	public void updatePassword(String userId, String oldPassword, String newPassword) throws Exception {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserController.UserNotFoundException("Usuario no encontrado"));

		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			throw new Exception("La contraseña actual es incorrecta");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}


	public void deleteUser(String userId) {
		userRepository.deleteById(userId);
	}
}
