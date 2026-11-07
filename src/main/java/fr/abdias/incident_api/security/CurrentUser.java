
package fr.abdias.incident_api.security;

public record CurrentUser(Long id, String email, boolean admin) {
}
