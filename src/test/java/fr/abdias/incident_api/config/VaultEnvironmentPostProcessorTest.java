
package fr.abdias.incident_api.config;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VaultEnvironmentPostProcessorTest {

	private final VaultEnvironmentPostProcessor postProcessor = new VaultEnvironmentPostProcessor();

	@Test
	void shouldMapKnownVaultKeysToEnvironmentPropertyNames() throws Exception {
		String vaultResponse = """
				{
				  "data": {
				    "data": {
				      "db-password": "secret-db-password",
				      "jwt-secret": "secret-jwt-key",
				      "admin-email": "admin@example.com",
				      "admin-password": "secret-admin-password",
				      "unrelated-key": "ignored"
				    }
				  }
				}
				""";

		Map<String, Object> secrets = VaultEnvironmentPostProcessor.parseSecrets(vaultResponse);

		assertThat(secrets).containsEntry("DB_PASSWORD", "secret-db-password")
				.containsEntry("JWT_SECRET", "secret-jwt-key").containsEntry("ADMIN_EMAIL", "admin@example.com")
				.containsEntry("ADMIN_PASSWORD", "secret-admin-password").doesNotContainKey("unrelated-key");
	}

	@Test
	void shouldDoNothingWhenVaultAddrIsMissing() {
		MockEnvironment environment = new MockEnvironment();
		int propertySourceCountBefore = environment.getPropertySources().size();

		postProcessor.postProcessEnvironment(environment, null);

		assertThat(environment.getPropertySources()).hasSize(propertySourceCountBefore);
	}

	@Test
	void shouldDoNothingWhenVaultTokenIsMissing() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("VAULT_ADDR", "http://vault:8200");
		int propertySourceCountBefore = environment.getPropertySources().size();

		postProcessor.postProcessEnvironment(environment, null);

		assertThat(environment.getPropertySources()).hasSize(propertySourceCountBefore);
	}
}
