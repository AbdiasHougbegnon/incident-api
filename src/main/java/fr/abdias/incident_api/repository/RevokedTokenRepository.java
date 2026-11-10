package fr.abdias.incident_api.repository;

import java.time.Instant;

import fr.abdias.incident_api.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    void deleteByExpiresAtBefore(Instant instant);
}
