
package fr.abdias.incident_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.abdias.incident_api.dto.AuthResponse;
import fr.abdias.incident_api.dto.LoginRequest;
import fr.abdias.incident_api.dto.RegisterRequest;
import fr.abdias.incident_api.service.AuthService;
import fr.abdias.incident_api.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import fr.abdias.incident_api.exception.EmailAlreadyExistsException;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request) {

        registrationService.register(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            String token = authService.authenticate(
                    request.getEmail(),
                    request.getPassword()
            );

            return ResponseEntity.ok(new AuthResponse(token));

        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @ExceptionHandler(EmailAlreadyExistsException.class)
public ResponseEntity<String> handleEmailAlreadyExists(
        EmailAlreadyExistsException exception) {

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(exception.getMessage());
}
}
