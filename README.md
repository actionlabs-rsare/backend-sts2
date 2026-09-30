# STS Backend

Java 21 + Spring Boot 3, built with **Gradle** (not Node.js — `gradlew` /
`gradlew.bat` is Gradle's wrapper script, the Java equivalent of `npm`).
REST API + Flyway migrations + Aurora-compatible PostgreSQL.

## ⚠️ You need a database. This repo does not include one.

This repo is **just the API source code**. There is no database engine or
data file anywhere in here — the app is a client that connects to a
PostgreSQL server running somewhere else. Before you run the app, you must
have a Postgres instance up and reachable. Pick **Option A** (Docker,
easiest, no install) or **Option B** (native Postgres) below.

If you skip this step, `gradlew bootRun` will start, try to connect, fail,
and retry forever — it looks stuck but it's actually just retrying the DB
connection. If that happens, check that Postgres is actually running first.

## Prerequisites

- JDK 17+ to run the Gradle wrapper itself (the build then provisions **JDK 21**
  via the Gradle toolchain for compiling/running the app — see
  `application.yml.note.md`).
- Docker Desktop (for Option A) **or** a local PostgreSQL install (Option B).

## Build

```bat
gradlew.bat clean build
```

Runs compile + tests + Jacoco coverage report. To skip tests for a faster
build:

```bat
gradlew.bat clean build -x test
```

## Database

The app expects a database and reads connection settings from environment
variables (defaults in `src/main/resources/application.yml`):

| Env var | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `sts` |
| `DB_USERNAME` | `sts` |
| `DB_PASSWORD` | `sts` |

### Option A — Docker Compose (recommended, no install needed)

This repo ships its own `docker-compose.yml` with just a Postgres service —
you don't need to install PostgreSQL at all, only Docker Desktop.

```bat
docker compose up -d
```

Starts Postgres 16 on port 5432 with the `sts` db/user already created.
Check it's healthy with `docker compose ps`. To stop it: `docker compose down`
(add `-v` to also delete the data volume and start fresh next time).

### Option B — local PostgreSQL install, on its own port

Useful if port 5432 is already taken by another Postgres instance (e.g. a
system-wide install). Initialize a separate data directory once:

```bat
"C:\Program Files\PostgreSQL\<version>\bin\initdb.exe" -D "%USERPROFILE%\.sts-tools\pgdata" -U sts -A trust -E UTF8
```

Start it on port 5433:

```bat
"C:\Program Files\PostgreSQL\<version>\bin\pg_ctl.exe" -D "%USERPROFILE%\.sts-tools\pgdata" -o "-p 5433" start
```

Create the database (default superuser `sts` has no password with `trust` auth):

```bat
"C:\Program Files\PostgreSQL\<version>\bin\psql.exe" -h localhost -p 5433 -U sts -d postgres -c "CREATE DATABASE sts;"
```

Stop it later with:

```bat
"C:\Program Files\PostgreSQL\<version>\bin\pg_ctl.exe" -D "%USERPROFILE%\.sts-tools\pgdata" stop
```

## Run the app

Dev profile enables the dev sign-in / role switcher (SECURITY-09/12 — never
enable this outside dev).

```bat
set SPRING_PROFILES_ACTIVE=dev
set STS_DEV_AUTH_ENABLED=true
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=sts
set DB_USERNAME=sts
set DB_PASSWORD=sts
gradlew.bat bootRun
```

(Adjust `DB_PORT` to `5433` etc. if using Option B above.)

Or just run `run-dev.bat` (Windows), which sets these same defaults and
starts the app in one step — edit the `DB_PORT` line in it first if you're
on Option B.

On startup, Flyway applies all migrations automatically and the app listens
on **http://localhost:8080** (override with `APP_PORT`).

## Swagger / OpenAPI

Once running:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Health check:** http://localhost:8080/actuator/health

Most `/api/**` endpoints are behind the access-matrix middleware (deny by
default, SL-005) and need a bearer token. In dev, get one first:

```
POST /api/auth/dev-login
Content-Type: application/json

{"role":"Admin"}
```

Valid `role` values (case-sensitive): `Admin`, `User`, `FirstLevelApprover`,
`SecondLevelApprover`, `TeamLeader`.

The response includes a `token`. In Swagger UI, use "Try it out" on any
protected endpoint and add the header:

```
Authorization: Bearer <token>
```

## Tests

```bat
gradlew.bat test jacocoTestReport jacocoTestCoverageVerification
```

Coverage gate is 90% (`practices.md` O3), enforced by
`jacocoTestCoverageVerification`.

## Container image

```bat
docker build -t sts-backend .
```

Multi-stage build (`Dockerfile`), non-root user, JDK 21 base — see
`Dockerfile` for details. This builds the **app** image; it's separate from
`docker-compose.yml` in this repo, which only runs the **database** for local
dev. (There used to be a combined app+db+frontend compose file when this was
part of the monorepo — that lived at the monorepo root and isn't part of
this repo.)
