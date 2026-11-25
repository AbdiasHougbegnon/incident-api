
package fr.abdias.incident_api.config;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Lit les secrets (DB_PASSWORD, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD) depuis
 * Azure Key Vault au tout debut du demarrage de l'application, et les injecte
 * comme PropertySource prioritaire sous les memes noms que les variables
 * d'environnement classiques. Si AZURE_KEY_VAULT_URI est absente ou Key Vault
 * injoignable, ne fait rien : l'application retombe sur les variables
 * d'environnement habituelles (${DB_PASSWORD}, ${JWT_SECRET}, ...).
 *
 * Fonctionne avec VaultEnvironmentPostProcessor sans conflit : les deux sont
 * declenches par des variables d'environnement differentes et ne s'activent
 * donc jamais en meme temps dans un environnement donne.
 */
public class AzureKeyVaultEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(AzureKeyVaultEnvironmentPostProcessor.class);

	private static final Map<String, String> SECRET_NAME_TO_PROPERTY = Map.of("db-password", "DB_PASSWORD",
			"jwt-secret", "JWT_SECRET", "admin-email", "ADMIN_EMAIL", "admin-password", "ADMIN_PASSWORD");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String keyVaultUri = environment.getProperty("AZURE_KEY_VAULT_URI");

		if (keyVaultUri == null || keyVaultUri.isBlank()) {
			return;
		}

		try {
			SecretClient client = new SecretClientBuilder().vaultUrl(keyVaultUri)
					.credential(new DefaultAzureCredentialBuilder().build()).buildClient();

			Map<String, Object> secrets = fetchSecrets(client);
			environment.getPropertySources().addFirst(new MapPropertySource("azure-key-vault", secrets));
			log.info("Secrets charges depuis Azure Key Vault ({})", keyVaultUri);
		} catch (Exception exception) {
			log.warn(
					"Impossible de lire les secrets depuis Azure Key Vault ({}) : {}. "
							+ "Utilisation des variables d'environnement classiques.",
					keyVaultUri, exception.getMessage());
		}
	}

	static Map<String, Object> fetchSecrets(SecretClient client) {
		Map<String, Object> result = new HashMap<>();

		SECRET_NAME_TO_PROPERTY.forEach((secretName, propertyName) -> {
			try {
				result.put(propertyName, client.getSecret(secretName).getValue());
			} catch (ResourceNotFoundException notFound) {
				log.info("Secret '{}' absent du Key Vault, ignore (optionnel).", secretName);
			}
		});

		return result;
	}
}
