
package fr.abdias.incident_api.service;
import fr.abdias.incident_api.exception.IncidentNotFoundException;
import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public Incident createIncident(Incident incident) {
        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    @Transactional(readOnly = true)
public Incident getIncidentById(Long id) {
    return incidentRepository.findById(id)
            .orElseThrow(() -> new IncidentNotFoundException(id));
}
}
