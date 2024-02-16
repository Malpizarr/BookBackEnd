package com.example.book.Controller;

import com.example.book.Model.LoginRequest;
import com.example.book.Model.LoginResponse;
import com.example.book.Model.User;
import com.example.book.Service.UserService;
import com.example.book.Util.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User newUser) {
        try {
            User user = userService.register(newUser);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse) {
        try {
            LoginResponse response = userService.login(loginRequest.getUsername(), loginRequest.getPassword());

            User user = userService.findByUsername(loginRequest.getUsername());

            String refreshToken = jwtTokenUtil.createRefreshToken(user);

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 días

            //Asignar la cookie al objeto HttpServletResponse
            String cookieValue = String.format("%s; %s; %s; %s; %s; %s",
                    refreshCookie.getName() + "=" + refreshToken,
                    "Max-Age=" + refreshCookie.getMaxAge(),
                    "Path=" + refreshCookie.getPath(),
                    "HttpOnly",
                    "Secure",
                    "SameSite=None"); // Puedes cambiar a "Strict" según tus necesidades

            httpServletResponse.addHeader("Set-Cookie", cookieValue);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo(HttpServletRequest request) {
        String jwt = extractJwtFromRequest(request);
        if (jwt == null) {
            return ResponseEntity.status(401).body("Invalid JWT token");
        }

        String username = jwtTokenUtil.getUsernameFromToken(jwt);
        UserDetails userDetails = userService.loadUserByUsername(username);
        if (!jwtTokenUtil.validateToken(jwt, userDetails)) {
            return ResponseEntity.status(401).body("Invalid JWT token");
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        return ResponseEntity.ok(user);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse httpServletResponse) {
        String refreshToken = null;

        // Extraer el refresh token de las cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        System.out.println("Refresh token: " + refreshToken);

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token no proporcionado");
        }

        try {
            String userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
            User user = userService.findUserById(userId);
            UserDetails userDetails = userService.loadUserByUsername(user.getUsername());

            if (!jwtTokenUtil.validateRefreshToken(refreshToken, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token inválido");
            }

            String newAccessToken = jwtTokenUtil.createToken(user);

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 días

            //Asignar la cookie al objeto HttpServletResponse
            String cookieValue = String.format("%s; %s; %s; %s; %s; %s",
                    refreshCookie.getName() + "=" + refreshToken,
                    "Max-Age=" + refreshCookie.getMaxAge(),
                    "Path=" + refreshCookie.getPath(),
                    "HttpOnly",
                    "Secure",
                    "SameSite=None"); // Puedes cambiar a "Strict" según tus necesidades

            httpServletResponse.addHeader("Set-Cookie", cookieValue);

            httpServletResponse.addCookie(refreshCookie);
            return ResponseEntity.ok(new LoginResponse(newAccessToken));
        } catch (JwtException e) {
            // Manejar específicamente las excepciones relacionadas con JWT
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error en el token JWT: " + e.getMessage());
        } catch (Exception e) {
            // Manejar otras excepciones no específicas
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor");
        }
    }

    @GetMapping("/set-cookie")
    public void setCookie(HttpServletResponse response, @RequestParam String token, @RequestParam String refreshToken, @RequestParam String username) throws IOException {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 días

        //Asignar la cookie al objeto HttpServletResponse
        String cookieValue = String.format("%s; %s; %s; %s; %s; %s",
                refreshCookie.getName() + "=" + refreshToken,
                "Max-Age=" + refreshCookie.getMaxAge(),
                "Path=" + refreshCookie.getPath(),
                "HttpOnly",
                "Secure",
                "SameSite=None");

        response.addHeader("Set-Cookie", cookieValue);
        response.sendRedirect("https://bookfront-delta.vercel.app?token=" + token + "&username=" + username);
    }



    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        String cookieValue = String.format("%s=%s; Max-Age=%s; Path=%s; HttpOnly; Secure; SameSite=None",
                cookie.getName(), cookie.getValue(), cookie.getMaxAge(), cookie.getPath());

        response.addHeader("Set-Cookie", cookieValue);

        return ResponseEntity.ok("Logout successful");
    }


    private String extractJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
