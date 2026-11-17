package com.superapp.api.shared.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite los JWT firmados (HS256). Vencen en máximo 30 minutos (NFR-04). */
@Service
public class TokenService {

    private static final long MAX_MINUTES = 30;

    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder,
                        @Value("${superapp.jwt.expiration-minutes:30}") long minutes) {
        if (minutes < 1 || minutes > MAX_MINUTES) {
            throw new IllegalStateException("El JWT debe vencer entre 1 y " + MAX_MINUTES + " minutos");
        }
        this.encoder = encoder;
        this.ttl = Duration.ofMinutes(minutes);
    }

    public IssuedToken issue(Long userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("superapp")
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", email)
                .claim("roles", List.copyOf(roles))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
