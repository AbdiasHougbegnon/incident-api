
package fr.abdias.incident_api.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "revoked_tokens")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

	@Id
	@Column(length = 36)
	private String jti;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;
}
