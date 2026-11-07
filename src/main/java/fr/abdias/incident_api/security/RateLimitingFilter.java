
package fr.abdias.incident_api.security;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite le débit des requêtes sur /api/auth/** par adresse IP.
 * Implémentation en mémoire, fenêtre fixe : adaptée à une seule instance.
 * Un déploiement multi-instance nécessiterait un store partagé (ex. Redis).
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final String PROTECTED_PREFIX = "/api/auth/";

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!request.getServletPath().startsWith(PROTECTED_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr();
        if (isAllowed(key)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":429,\"detail\":\"Trop de requêtes, veuillez réessayer plus tard.\"}"
        );
    }

    private boolean isAllowed(String key) {
        long now = Instant.now().toEpochMilli();

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MILLIS) {
                return new Window(now, 1);
            }
            existing.count++;
            return existing;
        });

        return window.count <= MAX_REQUESTS_PER_WINDOW;
    }

    private static final class Window {
        private final long windowStart;
        private int count;

        private Window(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
