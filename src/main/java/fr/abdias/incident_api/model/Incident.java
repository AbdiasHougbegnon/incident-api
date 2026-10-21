package fr.abdias.incident_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères.")
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank(message = "La description est obligatoire.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    @Column(nullable = false, length = 2000)
    private String description;
 


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentPriority priority;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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