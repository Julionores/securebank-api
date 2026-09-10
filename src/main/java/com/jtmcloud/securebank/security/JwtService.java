package com.jtmcloud.securebank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Émission et validation des jetons JWT.
 *
 * La clé de signature ({@code security.jwt.secret}) DOIT provenir d'une variable
 * d'environnement / d'un coffre-fort de secrets (ex: Vault, AWS Secrets Manager) en
 * production — jamais commitée. Voir application.yml et SECURITY.md.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes:30}") long expirationMinutes
    ) {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret doit faire au moins 256 bits (32 caractères) - "
                            + "voir SECURITY.md, section gestion des secrets.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationMinutes * 60 * 1000;
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            Date expiration = extractClaim(token, Claims::getExpiration);
            return username.equals(userDetails.getUsername()) && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
