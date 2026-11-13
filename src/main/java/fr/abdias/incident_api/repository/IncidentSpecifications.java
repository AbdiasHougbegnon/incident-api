package fr.abdias.incident_api.repository;

import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class IncidentSpecifications {

	private IncidentSpecifications() {
	}

	public static Specification<Incident> hasStatus(IncidentStatus status) {
		return (root, query, builder) -> status == null ? null : builder.equal(root.get("status"), status);
	}

	public static Specification<Incident> hasPriority(IncidentPriority priority) {
		return (root, query, builder) -> priority == null ? null : builder.equal(root.get("priority"), priority);
	}
}
