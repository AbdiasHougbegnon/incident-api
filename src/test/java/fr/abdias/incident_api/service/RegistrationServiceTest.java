
package fr.abdias.incident_api.service;

import fr.abdias.incident_api.exception.EmailAlreadyExistsException;
import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.repository.AppUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldRegisterUserWithNormalizedEmailAndEncodedPassword() {
        // Arrange
        String email = "  Abdias.Test@example.com  ";
        String normalizedEmail = "abdias.test@example.com";
        String password = "MotDePasseTest123!";
        String encodedPassword = "encoded-password";

        when(appUserRepository.existsByEmailIgnoreCase(normalizedEmail))
                .thenReturn(false);

        when(passwordEncoder.encode(password))
                .thenReturn(encodedPassword);

        // Act
        registrationService.register(email, password);

        // Assert
        ArgumentCaptor<AppUser> userCaptor =
                ArgumentCaptor.forClass(AppUser.class);

        verify(appUserRepository).save(userCaptor.capture());

        AppUser savedUser = userCaptor.getValue();

        assertEquals(normalizedEmail, savedUser.getEmail());
        assertEquals(encodedPassword, savedUser.getPasswordHash());
        assertEquals("ROLE_USER", savedUser.getRole());

        verify(passwordEncoder).encode(password);
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        // Arrange
        String email = "abdias.test@example.com";

        when(appUserRepository.existsByEmailIgnoreCase(email))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                EmailAlreadyExistsException.class,
                () -> registrationService.register(
                        email,
                        "MotDePasseTest123!"
                )
        );

        verify(appUserRepository, never()).save(any(AppUser.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }
}
