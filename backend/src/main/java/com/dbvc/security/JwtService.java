package com.dbvc.security;

import com.dbvc.auth.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${dbvc.jwt.issuer}") String issuer,
            @Value("${dbvc.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public GeneratedToken generateToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(jwsHeader, claims)
        ).getTokenValue();

        return new GeneratedToken(token, expiresAt);
    }

    public record GeneratedToken(String token, Instant expiresAt) {
    }
}