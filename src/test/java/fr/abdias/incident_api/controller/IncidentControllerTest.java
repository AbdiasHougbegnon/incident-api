
package fr.abdias.incident_api.controller;

import fr.abdias.incident_api.dto.IncidentResponse;
import fr.abdias.incident_api.exception.GlobalExceptionHandler;
import fr.abdias.incident_api.exception.IncidentNotFoundException;
import fr.abdias.incident_api.mapper.IncidentMapper;
import fr.abdias.incident_api.model.Incident;
import fr.abdias.incident_api.model.IncidentPriority;
import fr.abdias.incident_api.model.IncidentStatus;
import fr.abdias.incident_api.service.IncidentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    @Mock
    private IncidentService incidentService;

    @Mock
    private IncidentMapper incidentMapper;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new IncidentController(incidentService, incidentMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateIncident() throws Exception {
        Incident incident = Incident.builder()
                .id(1L)
                .title("Serveur inaccessible")
                .description("Le serveur ne répond plus.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .build();

        IncidentResponse response = IncidentResponse.builder()
                .id(1L)
                .title("Serveur inaccessible")
                .description("Le serveur ne répond plus.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .build();

        when(incidentService.createIncident(any(Incident.class)))
                .thenReturn(incident);
        when(incidentMapper.toResponse(incident)).thenReturn(response);

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Serveur inaccessible",
                                  "description": "Le serveur ne répond plus.",
                                  "status": "OPEN",
                                  "priority": "CRITICAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Serveur inaccessible"));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "description": "Description valide",
                                  "status": "OPEN",
                                  "priority": "MEDIUM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erreur de validation"));

        verifyNoInteractions(incidentService, incidentMapper);
    }

    
    @Test
    void shouldUpdateIncident() throws Exception {
        // 1. Préparer l'incident que le service doit retourner
        Incident incident = Incident.builder()
                .id(1L)
                .title("Serveur réparé")
                .description("Le serveur répond de nouveau.")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .build();

        // 2. Préparer la réponse DTO attendue par le contrôleur
        IncidentResponse response = IncidentResponse.builder()
                .id(1L)
                .title("Serveur réparé")
                .description("Le serveur répond de nouveau.")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .build();

        // 3. Simuler le comportement du service et du mapper
        when(incidentService.updateIncident(eq(1L), any(Incident.class)))
                .thenReturn(incident);

        when(incidentMapper.toResponse(incident))
                .thenReturn(response);

        // 4. Envoyer la requête HTTP PUT et vérifier la réponse
        mockMvc.perform(put("/api/incidents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "title": "Serveur réparé",
                                "description": "Le serveur répond de nouveau.",
                                "status": "RESOLVED",
                                "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Serveur réparé"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }


    @Test
    void shouldDeleteIncident() throws Exception {
        mockMvc.perform(delete("/api/incidents/1"))
                .andExpect(status().isNoContent());

        verify(incidentService).deleteIncident(1L);
    }

    @Test
    void shouldReturnNotFoundWhenIncidentDoesNotExist() throws Exception {
        when(incidentService.getIncidentById(999L))
                .thenThrow(new IncidentNotFoundException(999L));

        mockMvc.perform(get("/api/incidents/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Incident introuvable"));

        verify(incidentMapper, never()).toResponse(any(Incident.class));
    }

    @Test
    void shouldReturnAllIncidents() throws Exception {
        Incident incident = Incident.builder()
                .id(1L)
                .title("Serveur inaccessible")
                .description("Le serveur ne répond plus.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .build();

        IncidentResponse response = IncidentResponse.builder()
                .id(1L)
                .title("Serveur inaccessible")
                .description("Le serveur ne répond plus.")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .build();

        when(incidentService.getAllIncidents())
                .thenReturn(List.of(incident));
        when(incidentMapper.toResponse(incident)).thenReturn(response);

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Serveur inaccessible"));

        verify(incidentService).getAllIncidents();
        verify(incidentMapper).toResponse(incident);
    }

    
@Test
void shouldReturnBadRequestWhenUpdatingIncidentWithBlankTitle() throws Exception {
    mockMvc.perform(put("/api/incidents/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title": "   ",
                              "description": "Description valide",
                              "status": "OPEN",
                              "priority": "MEDIUM"
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Erreur de validation"));

    verifyNoInteractions(incidentService, incidentMapper);
}

}
