
CREATE TABLE revoked_tokens (
    jti VARCHAR(36) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
