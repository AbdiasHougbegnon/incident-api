
package fr.abdias.incident_api.service;

import fr.abdias.incident_api.exception.IncidentNotFoundException;
import fr.abdias.incident_api.model.AppUser;
import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import fr.abdias.incident_api.repository.AppUserRepository;
import fr.abdias.incident_api.repository.IncidentRepository;
import fr.abdias.incident_api.repository.IncidentSpecifications;
import fr.abdias.incident_api.security.CurrentUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AppUserRepository appUserRepository;

    public IncidentService(
            IncidentRepository incidentRepository,
            AppUserRepository appUserRepository) {
        this.incidentRepository = incidentRepository;
        this.appUserRepository = appUserRepository;
    }

    public Incident createIncident(Incident incident, CurrentUser currentUser) {
        AppUser reporter = appUserRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur authentifié introuvable : " + currentUser.email()
                ));

        incident.setReporter(reporter);

        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public Page<Incident> getIncidents(
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable) {

        Specification<Incident> specification = Specification
                .where(IncidentSpecifications.hasStatus(status))
                .and(IncidentSpecifications.hasPriority(priority));

        return incidentRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));
    }

    public Incident updateIncident(Long id, Incident updatedIncident, CurrentUser currentUser) {
        Incident existingIncident = getIncidentById(id);
        ensureCanModify(existingIncident, currentUser);

        existingIncident.setTitle(updatedIncident.getTitle());
        existingIncident.setDescription(updatedIncident.getDescription());
        existingIncident.setStatus(updatedIncident.getStatus());
        existingIncident.setPriority(updatedIncident.getPriority());

        return incidentRepository.save(existingIncident);
    }

    public void deleteIncident(Long id, CurrentUser currentUser) {
        Incident incident = getIncidentById(id);
        ensureCanModify(incident, currentUser);

        incidentRepository.delete(incident);
    }

    private void ensureCanModify(Incident incident, CurrentUser currentUser) {
        if (currentUser.admin()) {
            return;
        }

        AppUser reporter = incident.getReporter();
        if (reporter != null && reporter.getId().equals(currentUser.id())) {
            return;
        }

        throw new AccessDeniedException(
                "Vous n'êtes pas autorisé à modifier cet incident."
        );
    }
}
