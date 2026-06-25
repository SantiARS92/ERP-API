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

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.auth.jwt.secret}") String secret,
            @Value("${app.auth.jwt.expiration-ms}") long accessExpirationMs,
            @Value("${app.auth.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(CustomUserPrincipal principal) {
        return buildToken(principal, accessExpirationMs, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(CustomUserPrincipal principal) {
        return buildToken(principal, refreshExpirationMs, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(CustomUserPrincipal principal, long expiration, String tokenType) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("role", principal.getRole())
                .claim("displayName", principal.getDisplayName())
                .claim("professionalId", principal.getProfessionalId())
                .claim("tokenType", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public CustomUserPrincipal extractAccessPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        validateTokenType(claims, TOKEN_TYPE_ACCESS);
        return buildPrincipal(claims);
    }

    public CustomUserPrincipal extractRefreshPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        validateTokenType(claims, TOKEN_TYPE_REFRESH);
        return buildPrincipal(claims);
    }

    private void validateTokenType(Claims claims, String expectedType) {
        String tokenType = claims.get("tokenType", String.class);
        if (!expectedType.equals(tokenType)) {
            throw new JwtException("Tipo de token invalido");
        }
    }

    private CustomUserPrincipal buildPrincipal(Claims claims) {
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
