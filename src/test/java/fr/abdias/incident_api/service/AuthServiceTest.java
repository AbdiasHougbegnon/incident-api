
package fr.abdias.incident_api.service;

import fr.abdias.incident_api.model.AppRole;
import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.model.RevokedToken;
import fr.abdias.incident_api.repository.AppUserRepository;
import fr.abdias.incident_api.repository.RevokedTokenRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldGenerateTokenWhenCredentialsAreValid() {
        AppUser user = new AppUser();
        user.setEmail("abdias.test@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(AppRole.USER);

        when(appUserRepository.findByEmailIgnoreCase(
                "abdias.test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "MotDePasseTest123!", "encoded-password"))
                .thenReturn(true);

        // Le test du JWT sera complété avec un encodeur simulé.
        org.springframework.security.oauth2.jwt.Jwt jwt =
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue(
                        "test-token")
                        .header("alg", "HS256")
                        .subject("abdias.test@example.com")
                        .build();

        when(jwtEncoder.encode(any()))
                .thenReturn(jwt);

        String token = authService.authenticate(
                "abdias.test@example.com",
                "MotDePasseTest123!"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());
        verify(jwtEncoder).encode(any());
    }

    @Test
    void shouldIncludeUniqueJtiClaimInGeneratedToken() {
        AppUser user = new AppUser();
        user.setEmail("abdias.test@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(AppRole.USER);

        when(appUserRepository.findByEmailIgnoreCase("abdias.test@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("MotDePasseTest123!", "encoded-password"))
                .thenReturn(true);

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("abdias.test@example.com")
                .build();
        when(jwtEncoder.encode(any())).thenReturn(jwt);

        ArgumentCaptor<org.springframework.security.oauth2.jwt.JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(org.springframework.security.oauth2.jwt.JwtEncoderParameters.class);

        authService.authenticate("abdias.test@example.com", "MotDePasseTest123!");

        verify(jwtEncoder).encode(captor.capture());
        String jti = (String) captor.getValue().getClaims().getClaims().get("jti");
        assertNotNull(jti);
        assertFalse(jti.isBlank());
    }

    @Test
    void shouldSaveRevokedTokenOnLogout() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("abdias.test@example.com")
                .claim("jti", "token-id-123")
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        authService.logout(jwt);

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getJti()).isEqualTo("token-id-123");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void shouldRejectWrongPassword() {
        AppUser user = new AppUser();
        user.setEmail("abdias.test@example.com");
        user.setPasswordHash("encoded-password");

        when(appUserRepository.findByEmailIgnoreCase(
                "abdias.test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "MauvaisMotDePasse123!", "encoded-password"))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate(
                        "abdias.test@example.com",
                        "MauvaisMotDePasse123!"
                )
        );

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(appUserRepository.findByEmailIgnoreCase(
                "inconnu@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate(
                        "inconnu@example.com",
                        "MotDePasseTest123!"
                )
        );

        verify(jwtEncoder, never()).encode(any());
    }
}
