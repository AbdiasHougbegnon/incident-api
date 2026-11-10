
package fr.abdias.incident_api.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.model.RevokedToken;
import fr.abdias.incident_api.repository.AppUserRepository;
import fr.abdias.incident_api.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RevokedTokenRepository revokedTokenRepository;

    public String authenticate(String email, String password) {

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                    new BadCredentialsException(
                        "Identifiants invalides"
                    )
                );

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException(
                "Identifiants invalides"
            );
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256
        ).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public void logout(Jwt jwt) {
        revokedTokenRepository.save(new RevokedToken(jwt.getId(), jwt.getExpiresAt()));
    }
}
