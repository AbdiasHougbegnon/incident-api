
package fr.abdias.incident_api.config;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

	private final DatabaseUrlEnvironmentPostProcessor postProcessor = new DatabaseUrlEnvironmentPostProcessor();

	@Test
	void shouldTranslateDatabaseUrlIntoJdbcProperties() throws Exception {
		String databaseUrl = "postgres://demo_user:demo_pass@db.internal:5432/demo_db?sslmode=disable";

		Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.parse(databaseUrl);

		assertThat(properties).containsEntry("DB_URL", "jdbc:postgresql://db.internal:5432/demo_db?sslmode=disable")
				.containsEntry("DB_USERNAME", "demo_user").containsEntry("DB_PASSWORD", "demo_pass");
	}

	@Test
	void shouldDefaultToStandardPostgresPortWhenMissing() throws Exception {
		String databaseUrl = "postgres://demo_user:demo_pass@db.internal/demo_db";

		Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.parse(databaseUrl);

		assertThat(properties).containsEntry("DB_URL", "jdbc:postgresql://db.internal:5432/demo_db");
	}

	@Test
	void shouldDoNothingWhenDatabaseUrlIsMissing() {
		MockEnvironment environment = new MockEnvironment();
		int propertySourceCountBefore = environment.getPropertySources().size();

		postProcessor.postProcessEnvironment(environment, null);

		assertThat(environment.getPropertySources()).hasSize(propertySourceCountBefore);
	}

	@Test
	void shouldInjectPropertiesWhenDatabaseUrlIsPresent() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("DATABASE_URL", "postgres://demo_user:demo_pass@db.internal:5432/demo_db");

		postProcessor.postProcessEnvironment(environment, null);

		assertThat(environment.getProperty("DB_USERNAME")).isEqualTo("demo_user");
	}
}
