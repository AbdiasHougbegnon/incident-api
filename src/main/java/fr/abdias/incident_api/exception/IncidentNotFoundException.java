
package fr.abdias.incident_api.exception;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(Long id) {
        super("Incident introuvable avec l'identifiant : " + id);
    }
}
