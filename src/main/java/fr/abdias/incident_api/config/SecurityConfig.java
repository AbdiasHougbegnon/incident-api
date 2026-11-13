
package fr.abdias.incident_api.config;

import fr.abdias.incident_api.security.RateLimitingFilter;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	private final JwtAuthenticationConverter jwtAuthenticationConverter;
	private final RateLimitingFilter rateLimitingFilter;

	public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter,
			RateLimitingFilter rateLimitingFilter) {
		this.jwtAuthenticationConverter = jwtAuthenticationConverter;
		this.rateLimitingFilter = rateLimitingFilter;
	}

	/**
	 * Les endpoints Actuator vivent sur le port de gestion separe (9001, non expose
	 * a l'hote) : l'isolation reseau est leur mecanisme de protection, pas une
	 * authentification applicative (un scraper Prometheus ne peut pas se
	 * reconnecter avec un JWT qui expire toutes les heures). Sans cette chaine
	 * dediee, Spring Boot applique par defaut sa propre securite restrictive au
	 * contexte de gestion (ManagementWebSecurityAutoConfiguration).
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(EndpointRequest.toAnyEndpoint())
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.csrf(AbstractHttpConfigurer::disable);

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/auth/register", "/api/auth/login", "/swagger-ui/**", "/v3/api-docs/**")
						.permitAll().anyRequest().authenticated())
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
