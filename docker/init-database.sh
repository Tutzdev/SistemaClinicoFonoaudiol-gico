#!/usr/bin/env bash
set -eu

: "${POSTGRES_USER:?PostgreSQL administrator is required}"
: "${POSTGRES_DB:?Application database is required}"
: "${SINAPSE_DB_USER:?Application database user is required}"
: "${SINAPSE_DB_PASSWORD:?Application database password is required}"

if [ "$SINAPSE_DB_USER" = "$POSTGRES_USER" ]; then
    printf '%s\n' 'The application must use a different role from the PostgreSQL administrator.' >&2
    exit 1
fi
case "$SINAPSE_DB_USER" in
    pg_*)
        printf '%s\n' 'Application role names beginning with pg_ are reserved.' >&2
        exit 1
        ;;
esac

# Read secrets from the environment inside psql, not from process arguments.
# Quoted heredoc + psql variables + format(%I, %L) keep values out of SQL syntax.
psql --no-psqlrc --quiet --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
\getenv app_user SINAPSE_DB_USER
\getenv app_password SINAPSE_DB_PASSWORD
\getenv app_database POSTGRES_DB
SET log_statement = 'none';
SET log_min_error_statement = 'panic';
SELECT format(
    'CREATE ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L',
    :'app_user', :'app_password'
) \gexec
SELECT format('ALTER DATABASE %I OWNER TO %I', :'app_database', :'app_user') \gexec
SELECT format('GRANT USAGE, CREATE ON SCHEMA public TO %I', :'app_user') \gexec
SQL
