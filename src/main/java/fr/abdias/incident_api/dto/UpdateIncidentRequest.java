
package fr.abdias.incident_api.dto;

import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Données nécessaires à la modification d'un incident")
public class UpdateIncidentRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères.")
    private String title;

    @NotBlank(message = "La description est obligatoire.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    private String description;

    @NotNull(message = "Le statut est obligatoire.")
    private IncidentStatus status;

    @NotNull(message = "La priorité est obligatoire.")
    private IncidentPriority priority;
}
