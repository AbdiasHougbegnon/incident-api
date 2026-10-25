
package fr.abdias.incident_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Représente un incident informatique")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
        description = "Identifiant unique de l'incident",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères.")
    @Column(nullable = false, length = 150)
    @Schema(
        description = "Titre de l'incident",
        example = "Serveur inaccessible",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String title;

    @NotBlank(message = "La description est obligatoire.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    @Column(nullable = false, length = 2000)
    @Schema(
        description = "Description détaillée de l'incident",
        example = "Le serveur de production ne répond plus.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(
        description = "État actuel de l'incident",
        example = "OPEN"
    )
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(
        description = "Niveau de priorité de l'incident",
        example = "CRITICAL"
    )
    private IncidentPriority priority;

    @Column(nullable = false, updatable = false)
    @Schema(
        description = "Date et heure de création de l'incident",
        accessMode = Schema.AccessMode.READ_ONLY,
        example = "2026-10-24T14:30:00"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Date et heure de la dernière modification",
        accessMode = Schema.AccessMode.READ_ONLY,
        example = "2026-10-24T15:00:00"
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;

        if (status == null) {
            status = IncidentStatus.OPEN;
        }

        if (priority == null) {
            priority = IncidentPriority.MEDIUM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
