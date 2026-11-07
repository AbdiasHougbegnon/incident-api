
ALTER TABLE incidents ADD COLUMN reporter_id BIGINT;

ALTER TABLE incidents
    ADD CONSTRAINT fk_incidents_reporter
    FOREIGN KEY (reporter_id) REFERENCES app_users (id);
