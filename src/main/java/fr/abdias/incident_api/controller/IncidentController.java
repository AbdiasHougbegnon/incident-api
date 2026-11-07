
package fr.abdias.incident_api.controller;

import fr.abdias.incident_api.dto.CreateIncidentRequest;
import fr.abdias.incident_api.dto.IncidentResponse;
import fr.abdias.incident_api.mapper.IncidentMapper;
import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import fr.abdias.incident_api.security.CurrentUser;
import fr.abdias.incident_api.security.CurrentUserResolver;
import fr.abdias.incident_api.service.IncidentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    private final CurrentUserResolver currentUserResolver;

    public IncidentController(
            IncidentService incidentService,
            IncidentMapper incidentMapper,
            CurrentUserResolver currentUserResolver) {
        this.incidentService = incidentService;
        this.incidentMapper = incidentMapper;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(
        summary = "Créer un incident",
        description = "Crée un nouvel incident informatique. Le créateur devient le propriétaire (reporter) de l'incident."
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
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication) {

        Incident incident = Incident.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getStatus())
            .priority(request.getPriority())
            .build();

        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        Incident createdIncident = incidentService.createIncident(incident, currentUser);

        return incidentMapper.toResponse(createdIncident);
    }

    @Operation(
        summary = "Lister les incidents",
        description = "Retourne une page d'incidents, filtrable par statut et priorité."
    )
    @GetMapping
    @ApiResponse(
        responseCode = "200",
        description = "Page d'incidents retournée"
    )
    public PagedModel<IncidentResponse> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {

        Page<IncidentResponse> page = incidentService
                .getIncidents(status, priority, pageable)
                .map(incidentMapper::toResponse);

        return new PagedModel<>(page);
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
        description = "Met à jour le titre, la description, le statut et la priorité d'un incident existant. Réservé au propriétaire de l'incident ou à un administrateur."
    )
    @PutMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incident modifié avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Incident introuvable")
    })
    public IncidentResponse updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request,
            Authentication authentication) {

        Incident updatedIncident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .build();

        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        Incident incident = incidentService.updateIncident(id, updatedIncident, currentUser);

        return incidentMapper.toResponse(incident);
    }



    @Operation(
        summary = "Supprimer un incident",
        description = "Supprime un incident existant à partir de son identifiant. Réservé au propriétaire de l'incident ou à un administrateur."
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Incident supprimé avec succès"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Incident introuvable")
    })
    public void deleteIncident(@PathVariable Long id, Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        incidentService.deleteIncident(id, currentUser);
    }
}
