
package fr.abdias.incident_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Incident API",
        version = "1.0.0",
        description = "API REST de gestion des incidents informatiques."
    )
)
public class OpenApiConfig {
}
