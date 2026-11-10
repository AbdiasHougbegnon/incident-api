
package fr.abdias.incident_api.security;

import fr.abdias.incident_api.repository.RevokedTokenRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotRevokedJwtValidatorTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private NotRevokedJwtValidator validator;

    private Jwt jwtWithId(String jti) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("user@example.com")
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void shouldSucceedWhenTokenIsNotRevoked() {
        when(revokedTokenRepository.existsById("token-id-1")).thenReturn(false);

        OAuth2TokenValidatorResult result = validator.validate(jwtWithId("token-id-1"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldFailWhenTokenIsRevoked() {
        when(revokedTokenRepository.existsById("token-id-2")).thenReturn(true);

        OAuth2TokenValidatorResult result = validator.validate(jwtWithId("token-id-2"));

        assertThat(result.hasErrors()).isTrue();
    }
}
