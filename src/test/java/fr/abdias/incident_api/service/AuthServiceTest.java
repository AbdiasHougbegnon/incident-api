
package fr.abdias.incident_api.service;

import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.repository.AppUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.util.Optional;

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

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldGenerateTokenWhenCredentialsAreValid() {
        AppUser user = new AppUser();
        user.setEmail("abdias.test@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole("ROLE_USER");

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
