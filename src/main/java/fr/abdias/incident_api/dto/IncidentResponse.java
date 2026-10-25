
package fr.abdias.incident_api.dto;

import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Données d'un incident retournées par l'API")
public class IncidentResponse {

    @Schema(description = "Identifiant unique", example = "1")
    private Long id;

    @Schema(description = "Titre de l'incident", example = "Serveur inaccessible")
    private String title;

    @Schema(description = "Description de l'incident")
    private String description;

    @Schema(description = "État de l'incident", example = "OPEN")
    private IncidentStatus status;

    @Schema(description = "Priorité de l'incident", example = "CRITICAL")
    private IncidentPriority priority;

    @Schema(description = "Date de création")
    private LocalDateTime createdAt;

    @Schema(description = "Date de dernière modification")
    private LocalDateTime updatedAt;
}
