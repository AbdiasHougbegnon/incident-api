
package fr.abdias.incident_api.security;

import java.time.Instant;

import fr.abdias.incident_api.repository.RevokedTokenRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RevokedTokenCleanupJob {

    private final RevokedTokenRepository revokedTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredEntries() {
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
