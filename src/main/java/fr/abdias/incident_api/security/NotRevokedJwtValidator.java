
package fr.abdias.incident_api.security;

import fr.abdias.incident_api.repository.RevokedTokenRepository;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class NotRevokedJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final RevokedTokenRepository revokedTokenRepository;

    public NotRevokedJwtValidator(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jti = token.getId();

        if (jti != null && revokedTokenRepository.existsById(jti)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "token_revoked",
                    "Ce jeton a été révoqué.",
                    null
            ));
        }

        return OAuth2TokenValidatorResult.success();
    }
}
