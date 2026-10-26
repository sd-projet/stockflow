
package com.stockflow.api.security;

import com.stockflow.api.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;
    private final SecretKey secretKey;

    public JwtService() {
        String secret = System.getenv("STOCKFLOW_JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "La variable STOCKFLOW_JWT_SECRET n'est pas définie."
            );
        }

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(AppUser user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, AppUser user) {
        Claims claims = extractClaims(token);

        return claims.getSubject().equalsIgnoreCase(user.getEmail())
                && claims.getExpiration().after(new Date())
                && Boolean.TRUE.equals(user.getEnabled());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
