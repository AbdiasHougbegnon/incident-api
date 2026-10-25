
package fr.abdias.incident_api.dto;

import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Données nécessaires à la création d'un incident")
public class CreateIncidentRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères.")
    @Schema(description = "Titre de l'incident", example = "Serveur inaccessible", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "La description est obligatoire.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    @Schema(description = "Description de l'incident", example = "Le serveur de production ne répond plus.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "État initial de l'incident", example = "OPEN")
    private IncidentStatus status;

    @Schema(description = "Priorité de l'incident", example = "CRITICAL")
    private IncidentPriority priority;
}
