-- Database User Permissions Setup Script
-- 
-- PURPOSE: Setup script for local development and initial database configuration

-- USAGE:
--   1. Replace <DB_NAME> with your database name
--   2. Replace <DB_USER> with your database username
--   3. Run as PostgreSQL superuser: psql -U postgres -f docs/grant_permissions.sql

-- Connect to the database (replace with actual database name)
\c <DB_NAME>

-- Grant USAGE on schema
GRANT USAGE ON SCHEMA public TO <DB_USER>;

-- Grant SELECT, INSERT, UPDATE permissions on all existing tables
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO <DB_USER>;

-- Grant USAGE on all sequences (required for auto-increment IDs)
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO <DB_USER>;

-- Grant permissions on future tables (for new tables created later)
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
    GRANT SELECT, INSERT, UPDATE ON TABLES TO <DB_USER>;

ALTER DEFAULT PRIVILEGES IN SCHEMA public 
    GRANT USAGE, SELECT ON SEQUENCES TO <DB_USER>;

