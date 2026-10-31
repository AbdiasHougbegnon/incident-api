
package fr.abdias.incident_api.controller;

import fr.abdias.incident_api.exception.EmailAlreadyExistsException;
import fr.abdias.incident_api.service.AuthService;
import fr.abdias.incident_api.service.RegistrationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void shouldReturn201WhenRegistrationSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "nouveau@example.com",
                      "password": "MotDePasseTest123!"
                    }
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        doThrow(new EmailAlreadyExistsException(
                "Cette adresse e-mail est déjà utilisée."))
                .when(registrationService)
                .register("existant@example.com", "MotDePasseTest123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "existant@example.com",
                      "password": "MotDePasseTest123!"
                    }
                    """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$").value(
                        "Cette adresse e-mail est déjà utilisée."));
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "pas-un-email",
                      "password": "MotDePasseTest123!"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WhenLoginSucceeds() throws Exception {
        when(authService.authenticate(
                "utilisateur@example.com",
                "MotDePasseTest123!"))
                .thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "utilisateur@example.com",
                      "password": "MotDePasseTest123!"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("fake-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
