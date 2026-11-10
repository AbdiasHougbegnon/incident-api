
package fr.abdias.incident_api.security;

import fr.abdias.incident_api.repository.RevokedTokenRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevokedTokenCleanupJobTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private RevokedTokenCleanupJob cleanupJob;

    @Test
    void shouldDeleteEntriesExpiredBeforeNow() {
        Instant before = Instant.now();

        cleanupJob.purgeExpiredEntries();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(revokedTokenRepository).deleteByExpiresAtBefore(captor.capture());

        assertThat(captor.getValue()).isAfterOrEqualTo(before);
    }
}
