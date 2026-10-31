
package fr.abdias.incident_api.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import fr.abdias.incident_api.exception.EmailAlreadyExistsException;
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
    throw new EmailAlreadyExistsException(
        "Cette adresse e-mail est déjà utilisée."
    );}

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");

        appUserRepository.save(user);
    }
}
