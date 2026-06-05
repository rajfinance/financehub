-- Add first and last name to client_users (PostgreSQL)
-- Run once on prod/dev: psql ... -f client_users_names_migration.sql

ALTER TABLE client_users ADD COLUMN IF NOT EXISTS first_name varchar(80);
ALTER TABLE client_users ADD COLUMN IF NOT EXISTS last_name varchar(80);
