
package fr.abdias.incident_api.security;

import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.repository.AppUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserResolverTest {

	@Mock
	private AppUserRepository appUserRepository;

	@InjectMocks
	private CurrentUserResolver currentUserResolver;

	@Test
	void shouldResolveRegularUserAsNonAdmin() {
		AppUser user = new AppUser();
		user.setId(1L);
		user.setEmail("user1@example.com");

		when(appUserRepository.findByEmailIgnoreCase("user1@example.com")).thenReturn(Optional.of(user));

		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
		var authentication = new UsernamePasswordAuthenticationToken("user1@example.com", null, authorities);

		CurrentUser result = currentUserResolver.resolve(authentication);

		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.email()).isEqualTo("user1@example.com");
		assertThat(result.admin()).isFalse();
	}

	@Test
	void shouldResolveAdminUserAsAdmin() {
		AppUser user = new AppUser();
		user.setId(2L);
		user.setEmail("admin@example.com");

		when(appUserRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));

		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
		var authentication = new UsernamePasswordAuthenticationToken("admin@example.com", null, authorities);

		CurrentUser result = currentUserResolver.resolve(authentication);

		assertThat(result.admin()).isTrue();
	}

	@Test
    void shouldThrowWhenAuthenticatedUserIsUnknown() {
        when(appUserRepository.findByEmailIgnoreCase("ghost@example.com"))
                .thenReturn(Optional.empty());

        var authentication = new UsernamePasswordAuthenticationToken(
                "ghost@example.com", null, List.of());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> currentUserResolver.resolve(authentication));
    }
}
