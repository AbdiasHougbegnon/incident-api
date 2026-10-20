
package fr.abdias.incident_api.service;

import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import fr.abdias.incident_api.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.abdias.incident_api.exception.IncidentNotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void shouldCreateIncident() {
        // Arrange
        Incident incident = Incident.builder()
                .title("Serveur inaccessible")
                .description("Le serveur ne répond plus.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .build();

        when(incidentRepository.save(incident)).thenReturn(incident);

        // Act
        Incident result = incidentService.createIncident(incident);

        // Assert
        assertThat(result).isSameAs(incident);
        verify(incidentRepository).save(incident);
    }

    @Test
    void shouldReturnAllIncidents() {
        // Arrange
        Incident first = Incident.builder()
                .title("Panne réseau")
                .description("Connexion indisponible.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .build();

        Incident second = Incident.builder()
                .title("Erreur applicative")
                .description("L'application ne démarre pas.")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.MEDIUM)
                .build();

        when(incidentRepository.findAll()).thenReturn(List.of(first, second));

        // Act
        List<Incident> result = incidentService.getAllIncidents();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(first, second);
        verify(incidentRepository).findAll();
    }

    
    @Test
    void shouldReturnIncidentWhenIdExists() {
        Incident incident = Incident.builder()
                .title("Panne réseau")
                .description("Connexion indisponible.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .build();

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(incident));

        Incident result = incidentService.getIncidentById(1L);

        assertThat(result).isSameAs(incident);
        verify(incidentRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncidentById(99L))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident introuvable avec l'identifiant : 99");

        verify(incidentRepository).findById(99L);
    }

}
