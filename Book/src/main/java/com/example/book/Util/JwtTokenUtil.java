package com.example.book.Util;

import com.example.book.Model.CustomUserDetails;
import com.example.book.Model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    private long validityInMilliseconds = 3600000; // 1 hour in milliseconds

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    public String createToken(User user) {
        Claims claims = Jwts.claims().setSubject(user.getId());
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("createdAt", user.getCreatedAt());
        claims.put("photoUrl", user.getPhotoUrl());

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("username", String.class);
    }

    public Date getCreationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("createdAt", Date.class); // Asume que el claim se llama "createdAt"
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String userIdFromToken = getUserIdFromToken(token);
            String userIdFromUserDetails = ((CustomUserDetails) userDetails).getUserId();

            return (userIdFromToken.equals(userIdFromUserDetails) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        try {
            final String userIdFromToken = getUserIdFromToken(token);
            String userIdFromUserDetails = ((CustomUserDetails) userDetails).getUserId();

            // Comprueba si el ID del usuario del token coincide con el ID del usuario de UserDetails y si el token no ha expirado
            return (userIdFromToken.equals(userIdFromUserDetails) && !isTokenExpired(token));
        } catch (Exception e) {
            System.out.println("Error al validar refresh token: " + e.getMessage());
            return false;
        }
    }




    private boolean isTokenExpired(String token) {
        final Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    public String getUserIdFromToken(String jwtToken) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwtToken)
                .getBody()
                .getSubject();
    }

    public String createRefreshToken(User user) {
        Claims claims = Jwts.claims().setSubject(user.getId());
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("createdAt", user.getCreatedAt());
        claims.put("photoUrl", user.getPhotoUrl());

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
