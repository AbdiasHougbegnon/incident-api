
package fr.abdias.incident_api.security;

import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.repository.AppUserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final AppUserRepository appUserRepository;

    public CurrentUser resolve(Authentication authentication) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur authentifié introuvable : " + authentication.getName()
                ));

        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_AUTHORITY::equals);

        return new CurrentUser(user.getId(), user.getEmail(), admin);
    }
}
