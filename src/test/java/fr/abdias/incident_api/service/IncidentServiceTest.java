
package fr.abdias.incident_api.service;

import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import fr.abdias.incident_api.repository.AppUserRepository;
import fr.abdias.incident_api.repository.IncidentRepository;
import fr.abdias.incident_api.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.abdias.incident_api.exception.IncidentNotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private IncidentService incidentService;

    private AppUser owner(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("owner" + id + "@example.com");
        return user;
    }

    private CurrentUser currentUser(Long id, boolean admin) {
        return new CurrentUser(id, "user" + id + "@example.com", admin);
    }

    @Test
    void shouldCreateIncidentAndAssignReporter() {
        Incident incident = Incident.builder()
                .title("Serveur inaccessible")
                .description("Le serveur ne répond plus.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .build();

        AppUser reporter = owner(1L);
        CurrentUser currentUser = currentUser(1L, false);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(incidentRepository.save(incident)).thenReturn(incident);

        Incident result = incidentService.createIncident(incident, currentUser);

        assertThat(result.getReporter()).isSameAs(reporter);
        verify(incidentRepository).save(incident);
    }

    @Test
    void shouldReturnPagedIncidents() {
        Incident first = Incident.builder()
                .title("Panne réseau")
                .description("Connexion indisponible.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<Incident> page = new PageImpl<>(List.of(first), pageable, 1);

        when(incidentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        Page<Incident> result = incidentService.getIncidents(
                IncidentStatus.OPEN, null, pageable);

        assertThat(result.getContent()).containsExactly(first);
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

    @Test
    void shouldUpdateIncidentWhenCurrentUserIsOwner() {
        AppUser reporter = owner(1L);
        Incident existingIncident = Incident.builder()
                .id(1L)
                .title("Ancien titre")
                .description("Ancienne description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .reporter(reporter)
                .build();

        Incident updatedIncident = Incident.builder()
                .title("Nouveau titre")
                .description("Nouvelle description")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .build();

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(existingIncident));
        when(incidentRepository.save(existingIncident))
                .thenReturn(existingIncident);

        Incident result = incidentService.updateIncident(
                1L, updatedIncident, currentUser(1L, false));

        assertThat(result.getTitle()).isEqualTo("Nouveau titre");
        assertThat(result.getDescription()).isEqualTo("Nouvelle description");
        assertThat(result.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(result.getPriority()).isEqualTo(IncidentPriority.HIGH);

        verify(incidentRepository).save(existingIncident);
    }

    @Test
    void shouldUpdateIncidentWhenCurrentUserIsAdmin() {
        Incident existingIncident = Incident.builder()
                .id(1L)
                .title("Ancien titre")
                .description("Ancienne description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .reporter(owner(1L))
                .build();

        Incident updatedIncident = Incident.builder()
                .title("Nouveau titre")
                .description("Nouvelle description")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .build();

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(existingIncident));
        when(incidentRepository.save(existingIncident))
                .thenReturn(existingIncident);

        Incident result = incidentService.updateIncident(
                1L, updatedIncident, currentUser(2L, true));

        assertThat(result.getTitle()).isEqualTo("Nouveau titre");
    }

    @Test
    void shouldRejectUpdateWhenCurrentUserIsNeitherOwnerNorAdmin() {
        Incident existingIncident = Incident.builder()
                .id(1L)
                .title("Ancien titre")
                .description("Ancienne description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .reporter(owner(1L))
                .build();

        Incident updatedIncident = Incident.builder()
                .title("Nouveau titre")
                .description("Nouvelle description")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .build();

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(existingIncident));

        assertThatThrownBy(() -> incidentService.updateIncident(
                1L, updatedIncident, currentUser(2L, false)))
                .isInstanceOf(AccessDeniedException.class);

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void shouldDeleteIncidentWhenCurrentUserIsOwner() {
        Incident incident = Incident.builder()
                .id(1L)
                .title("Incident à supprimer")
                .description("Description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .reporter(owner(1L))
                .build();

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(incident));

        incidentService.deleteIncident(1L, currentUser(1L, false));

        verify(incidentRepository).delete(incident);
    }

    @Test
    void shouldRejectDeleteWhenCurrentUserIsNeitherOwnerNorAdmin() {
        Incident incident = Incident.builder()
                .id(1L)
                .title("Incident à supprimer")
                .description("Description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .reporter(owner(1L))
                .build();

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.deleteIncident(1L, currentUser(2L, false)))
                .isInstanceOf(AccessDeniedException.class);

        verify(incidentRepository, never()).delete(any(Incident.class));
    }

    @Test
    void shouldRejectModificationOfLegacyIncidentWithoutReporterUnlessAdmin() {
        Incident legacyIncident = Incident.builder()
                .id(1L)
                .title("Incident historique")
                .description("Sans propriétaire")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .build();

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(legacyIncident));

        assertThatThrownBy(() -> incidentService.deleteIncident(1L, currentUser(1L, false)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingIncident() {
        when(incidentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.deleteIncident(99L, currentUser(1L, true)))
                .isInstanceOf(IncidentNotFoundException.class);

        verify(incidentRepository, never()).delete(any(Incident.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingIncident() {
        Incident updatedIncident = Incident.builder()
                .title("Nouveau titre")
                .description("Nouvelle description")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .build();

        when(incidentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> incidentService.updateIncident(99L, updatedIncident, currentUser(1L, true)))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident introuvable avec l'identifiant : 99");

        verify(incidentRepository, never()).save(any(Incident.class));
    }
}
