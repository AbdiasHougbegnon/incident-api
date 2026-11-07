
package fr.abdias.incident_api.security;

import fr.abdias.incident_api.model.AppRole;
import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.repository.AppUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateAdminWhenCredentialsProvidedAndNoAdminExists() {
        when(appUserRepository.existsByRole(AppRole.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("SuperSecret123!")).thenReturn("encoded");

        AdminAccountInitializer initializer = new AdminAccountInitializer(
                appUserRepository, passwordEncoder, "Admin@Example.com", "SuperSecret123!");

        initializer.run(null);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());

        AppUser saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(AppRole.ADMIN);
    }

    @Test
    void shouldSkipWhenAdminAlreadyExists() {
        when(appUserRepository.existsByRole(AppRole.ADMIN)).thenReturn(true);

        AdminAccountInitializer initializer = new AdminAccountInitializer(
                appUserRepository, passwordEncoder, "admin@example.com", "SuperSecret123!");

        initializer.run(null);

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void shouldSkipWhenCredentialsAreNotConfigured() {
        AdminAccountInitializer initializer = new AdminAccountInitializer(
                appUserRepository, passwordEncoder, "", "");

        initializer.run(null);

        verify(appUserRepository, never()).existsByRole(any());
        verify(appUserRepository, never()).save(any(AppUser.class));
    }
}
