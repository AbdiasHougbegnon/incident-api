
package fr.abdias.incident_api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Lit les secrets (DB_PASSWORD, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD) depuis
 * HashiCorp Vault au tout début du démarrage de l'application, et les injecte
 * comme PropertySource prioritaire sous les mêmes noms que les variables
 * d'environnement classiques. Si VAULT_ADDR/VAULT_TOKEN sont absents ou Vault
 * injoignable, ne fait rien : l'application retombe sur les variables
 * d'environnement habituelles (${DB_PASSWORD}, ${JWT_SECRET}, ...).
 */
public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(VaultEnvironmentPostProcessor.class);

	private static final Map<String, String> VAULT_KEY_TO_PROPERTY = Map.of("db-password", "DB_PASSWORD", "jwt-secret",
			"JWT_SECRET", "admin-email", "ADMIN_EMAIL", "admin-password", "ADMIN_PASSWORD");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String vaultAddr = environment.getProperty("VAULT_ADDR");
		String vaultToken = environment.getProperty("VAULT_TOKEN");

		if (vaultAddr == null || vaultAddr.isBlank() || vaultToken == null || vaultToken.isBlank()) {
			return;
		}

		try {
			String body = requestSecrets(vaultAddr, vaultToken);
			Map<String, Object> secrets = parseSecrets(body);
			environment.getPropertySources().addFirst(new MapPropertySource("vault", secrets));
			log.info("Secrets charges depuis HashiCorp Vault ({})", vaultAddr);
		} catch (Exception exception) {
			log.warn(
					"Impossible de lire les secrets depuis Vault ({}) : {}. "
							+ "Utilisation des variables d'environnement classiques.",
					vaultAddr, exception.getMessage());
		}
	}

	private String requestSecrets(String vaultAddr, String vaultToken) throws Exception {
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(vaultAddr + "/v1/secret/data/incident-api"))
				.header("X-Vault-Token", vaultToken).timeout(Duration.ofSeconds(3)).GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IllegalStateException("Vault a repondu HTTP " + response.statusCode());
		}

		return response.body();
	}

	static Map<String, Object> parseSecrets(String vaultResponseBody) throws Exception {
		JsonNode data = new ObjectMapper().readTree(vaultResponseBody).path("data").path("data");

		Map<String, Object> result = new HashMap<>();
		Iterator<Map.Entry<String, JsonNode>> fields = data.fields();

		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> field = fields.next();
			String propertyName = VAULT_KEY_TO_PROPERTY.get(field.getKey());
			if (propertyName != null) {
				result.put(propertyName, field.getValue().asText());
			}
		}

		return result;
	}
}
