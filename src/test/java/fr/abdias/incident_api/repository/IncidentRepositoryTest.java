
package fr.abdias.incident_api.repository;

import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void shouldSaveIncidentAndGenerateId() {
        // Arrange : préparer un incident
        Incident incident = Incident.builder()
                .title("Serveur inaccessible")
                .description("Le serveur de production ne répond plus.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .build();

        // Act : enregistrer l'incident
        Incident savedIncident = incidentRepository.save(incident);

        // Assert : vérifier le résultat
        assertThat(savedIncident.getId()).isNotNull();
        assertThat(savedIncident.getTitle())
                .isEqualTo("Serveur inaccessible");
        assertThat(savedIncident.getCreatedAt()).isNotNull();
    }
}
