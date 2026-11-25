
package fr.abdias.incident_api.config;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureKeyVaultEnvironmentPostProcessorTest {

	private final AzureKeyVaultEnvironmentPostProcessor postProcessor = new AzureKeyVaultEnvironmentPostProcessor();

	@Test
	void shouldDoNothingWhenAzureKeyVaultUriIsMissing() {
		MockEnvironment environment = new MockEnvironment();
		int propertySourceCountBefore = environment.getPropertySources().size();

		postProcessor.postProcessEnvironment(environment, null);

		assertThat(environment.getPropertySources()).hasSize(propertySourceCountBefore);
	}

	@Test
	void shouldMapKnownSecretNamesToEnvironmentPropertyNames() {
		SecretClient client = mock(SecretClient.class);
		mockSecret(client, "db-password", "secret-db-password");
		mockSecret(client, "jwt-secret", "secret-jwt-key");
		mockSecret(client, "admin-email", "admin@example.com");
		mockSecret(client, "admin-password", "secret-admin-password");

		Map<String, Object> secrets = AzureKeyVaultEnvironmentPostProcessor.fetchSecrets(client);

		assertThat(secrets).containsEntry("DB_PASSWORD", "secret-db-password")
				.containsEntry("JWT_SECRET", "secret-jwt-key").containsEntry("ADMIN_EMAIL", "admin@example.com")
				.containsEntry("ADMIN_PASSWORD", "secret-admin-password");
	}

	@Test
	void shouldIgnoreMissingOptionalSecretsWithoutFailing() {
		SecretClient client = mock(SecretClient.class);
		mockSecret(client, "db-password", "secret-db-password");
		mockSecret(client, "jwt-secret", "secret-jwt-key");
		when(client.getSecret("admin-email")).thenThrow(new ResourceNotFoundException("not found", null));
		when(client.getSecret("admin-password")).thenThrow(new ResourceNotFoundException("not found", null));

		Map<String, Object> secrets = AzureKeyVaultEnvironmentPostProcessor.fetchSecrets(client);

		assertThat(secrets).containsEntry("DB_PASSWORD", "secret-db-password")
				.containsEntry("JWT_SECRET", "secret-jwt-key").doesNotContainKey("ADMIN_EMAIL")
				.doesNotContainKey("ADMIN_PASSWORD");
	}

	private void mockSecret(SecretClient client, String secretName, String value) {
		KeyVaultSecret secret = mock(KeyVaultSecret.class);
		when(secret.getValue()).thenReturn(value);
		when(client.getSecret(secretName)).thenReturn(secret);
	}
}
