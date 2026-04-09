package es.pedro.pymes_backend.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.auth.jwt.secret}") String secret,
            @Value("${app.auth.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(CustomUserPrincipal principal) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("role", principal.getRole())
                .claim("displayName", principal.getDisplayName())
                .claim("professionalId", principal.getProfessionalId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public CustomUserPrincipal extractPrincipal(String token) {
        Claims claims = extractAllClaims(token);

        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String displayName = claims.get("displayName", String.class);
        Long professionalId = extractProfessionalId(claims);

        if (username == null || role == null || displayName == null) {
            throw new JwtException("Token incompleto");
        }

        return new CustomUserPrincipal(username, role, displayName, professionalId);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long extractProfessionalId(Claims claims) {
        Number professionalId = claims.get("professionalId", Number.class);
        return professionalId == null ? null : professionalId.longValue();
    }
}
