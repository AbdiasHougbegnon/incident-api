
UPDATE app_users SET role = 'ADMIN' WHERE role = 'ROLE_ADMIN';
UPDATE app_users SET role = 'USER' WHERE role = 'ROLE_USER';

ALTER TABLE app_users ALTER COLUMN role SET DEFAULT 'USER';
