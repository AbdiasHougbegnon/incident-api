
package fr.abdias.incident_api.controller;

import fr.abdias.incident_api.dto.CreateIncidentRequest;
import fr.abdias.incident_api.dto.IncidentResponse;
import fr.abdias.incident_api.mapper.IncidentMapper;
import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.service.IncidentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import fr.abdias.incident_api.dto.UpdateIncidentRequest;

@Tag(
    name = "Incidents",
    description = "Gestion des incidents informatiques"
)
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    public IncidentController(
            IncidentService incidentService,
            IncidentMapper incidentMapper) {
        this.incidentService = incidentService;
        this.incidentMapper = incidentMapper;
    }

    @Operation(
        summary = "Créer un incident",
        description = "Crée un nouvel incident informatique."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Données nécessaires pour créer un incident",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                name = "Incident critique",
                value = """
                    {
                      "title": "Serveur inaccessible",
                      "description": "Le serveur de production ne répond plus.",
                      "status": "OPEN",
                      "priority": "CRITICAL"
                    }
                    """
            )
        )
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Incident créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public IncidentResponse createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {

        Incident incident = Incident.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getStatus())
            .priority(request.getPriority())
            .build();

        Incident createdIncident = incidentService.createIncident(incident);

        return incidentMapper.toResponse(createdIncident);
    }

    @Operation(
        summary = "Lister les incidents",
        description = "Retourne la liste de tous les incidents enregistrés."
    )
    @GetMapping
    @ApiResponse(
        responseCode = "200",
        description = "Liste des incidents retournée"
    )
    public List<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents()
            .stream()
            .map(incidentMapper::toResponse)
            .toList();
    }

    @Operation(
        summary = "Consulter un incident",
        description = "Retourne les détails d'un incident à partir de son identifiant."
    )
    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incident trouvé"),
        @ApiResponse(responseCode = "404", description = "Incident introuvable")
    })
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        return incidentMapper.toResponse(
            incidentService.getIncidentById(id)
        );
    }

    
    @Operation(
        summary = "Modifier un incident",
        description = "Met à jour le titre, la description, le statut et la priorité d'un incident existant."
    )
    @PutMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incident modifié avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Incident introuvable")
    })
    public IncidentResponse updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request) {

        Incident updatedIncident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .build();

        Incident incident = incidentService.updateIncident(id, updatedIncident);

        return incidentMapper.toResponse(incident);
    }


    
    @Operation(
        summary = "Supprimer un incident",
        description = "Supprime un incident existant à partir de son identifiant."
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Incident supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Incident introuvable")
    })
    public void deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
    }
}
