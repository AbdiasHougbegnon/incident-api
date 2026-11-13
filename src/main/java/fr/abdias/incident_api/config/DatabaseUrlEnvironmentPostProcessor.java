
package fr.abdias.incident_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduit une variable DATABASE_URL (format postgres://user:pass@host:port/db,
 * tel que généré automatiquement par `fly postgres attach`) en DB_URL (format
 * JDBC), DB_USERNAME et DB_PASSWORD. Permet de déployer sur Fly.io sans jamais
 * avoir à copier/coller le mot de passe généré dans une commande séparée : Fly
 * le stocke lui-même comme secret chiffré, et l'application le lit directement
 * au démarrage. Ne fait rien si DATABASE_URL est absent (comportement
 * local/Docker inchangé).
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String databaseUrl = environment.getProperty("DATABASE_URL");

		if (databaseUrl == null || databaseUrl.isBlank()) {
			return;
		}

		try {
			Map<String, Object> properties = parse(databaseUrl);
			environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", properties));
			log.info("DB_URL/DB_USERNAME/DB_PASSWORD derives depuis DATABASE_URL.");
		} catch (Exception exception) {
			log.warn(
					"Impossible d'interpreter DATABASE_URL : {}. "
							+ "Utilisation des variables DB_URL/DB_USERNAME/DB_PASSWORD classiques.",
					exception.getMessage());
		}
	}

	static Map<String, Object> parse(String databaseUrl) throws Exception {
		URI uri = new URI(databaseUrl);

		String userInfo = uri.getUserInfo();
		if (userInfo == null || !userInfo.contains(":")) {
			throw new IllegalArgumentException("DATABASE_URL ne contient pas d'identifiants utilisateur:mot_de_passe.");
		}

		String[] credentials = userInfo.split(":", 2);
		String username = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8);
		String password = URLDecoder.decode(credentials[1], StandardCharsets.UTF_8);

		int port = uri.getPort() == -1 ? 5432 : uri.getPort();
		String database = uri.getPath().replaceFirst("^/", "");

		// Le pilote JDBC PostgreSQL essaie le TLS par defaut : il faut reporter
		// explicitement sslmode=disable (ou tout autre parametre) depuis
		// DATABASE_URL, sinon la negociation SSL echoue contre une base interne
		// Fly qui n'en fait pas (handshake refuse).
		String query = uri.getQuery();
		String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + database
				+ (query != null && !query.isBlank() ? "?" + query : "");

		Map<String, Object> result = new HashMap<>();
		result.put("DB_URL", jdbcUrl);
		result.put("DB_USERNAME", username);
		result.put("DB_PASSWORD", password);
		return result;
	}
}
