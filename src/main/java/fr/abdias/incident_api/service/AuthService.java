
package fr.abdias.incident_api.service;

import java.time.Instant;
import java.util.Date;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

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
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", user.getRole())
                .build();

        JwsHeader header = JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256
        ).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
