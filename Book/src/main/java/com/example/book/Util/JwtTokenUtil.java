package com.example.book.Util;

import com.example.book.Model.CustomUserDetails;
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

    public String createToken(String userId, String username, String email, Date createdAt, String photoUrl) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("email", email);
        claims.put("createdAt", createdAt);
        claims.put("photoUrl", photoUrl);

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

        return claims.get("username", String.class); // Asume que el claim se llama "username"
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
        final String userIdFromToken = getUserIdFromToken(token);
        String userIdFromUserDetails = ((CustomUserDetails) userDetails).getUserId();

        return (userIdFromToken.equals(userIdFromUserDetails) && !isTokenExpired(token));
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
}
