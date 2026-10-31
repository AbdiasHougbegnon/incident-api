
package fr.abdias.incident_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.abdias.incident_api.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
