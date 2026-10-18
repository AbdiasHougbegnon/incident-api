package fr.abdias.incident_api.repository;

import fr.abdias.incident_api.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
}