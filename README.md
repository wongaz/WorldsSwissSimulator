# Worlds Swiss Simulator

Simulator for the League of Legends Worlds Swiss stage, with a **Compose for Web**
browser UI, a Kotlin/Ktor backend, and PostgreSQL-backed run history. The frontend
uses Kotlin/JS and Compose HTML (DOM elements), not a standalone desktop app.

## Requirements

- JDK 25, with `JAVA_HOME` pointing to its installation.
- Gradle downloads the Node.js/Yarn tooling needed to compile the Kotlin browser
  client; a separate frontend server or manually installed Node.js is not required.
- Docker Desktop with Linux containers for the local PostgreSQL service and
  integration tests. An existing PostgreSQL server can be used by the application
  instead.

## Run the web application

From PowerShell in the repository root:

```powershell
$env:SIM_DB_PASSWORD = '<your-local-database-password>'
docker compose up -d --wait
.\gradlew.bat run
```

Open **http://localhost:8080** in your browser. This command builds the production
web bundle and starts the backend, which serves both the UI and JSON API from the
same origin. No desktop window is launched. The first build downloads frontend
dependencies and can take longer. Set `SIM_HTTP_PORT` to use a different port.

The server binds only to `127.0.0.1`. It is a local, unauthenticated application,
not a public multi-user deployment. Database credentials belong in the backend's
environment and are never sent to the browser.

Use the same password when restarting the application. PostgreSQL initializes the
database password only when its data volume is first created; changing the
environment variable does not change an existing database user's password.

The UI lets you choose a bundled team dataset and 1 to 1,000,000 iterations,
launch a simulation, and browse the latest 100 completed runs. Select a saved run
to view its per-team qualification counts and percentages. `worlds2024.yml` is
currently the complete 16-team dataset; `basic_worlds.yml` is a one-team loader
fixture, not a valid Swiss tournament.

Simulation and database work run on the Kotlin backend. A run is shown as saved only
after its metadata and all team results have been committed to PostgreSQL.
Database failures are displayed in the UI rather than falling back to temporary
in-memory history. If the database is unavailable, start it and refresh. Missing
environment variables require setting them and restarting the backend process.

Keep the browser tab open while a simulation is running. The backend allows one
simulation at a time and reports a conflict if another tab tries to start one.
If a request loses its connection, the server may still complete and save the
run; refresh history before retrying to avoid duplicate runs.

The database stores a snapshot of the teams' names, regions, seeds, categories,
Elo ratings, and results, so changing a dataset does not change historical runs.
History contains completed, saved runs; interrupted or failed attempts are not
recorded as successful runs.

### Database connection

| Environment variable | Default |
| --- | --- |
| `SIM_DB_URL` | `jdbc:postgresql://localhost:5432/worlds_swiss` |
| `SIM_DB_USER` | `worlds_swiss` |
| `SIM_DB_PASSWORD` | Required; no default |

To use an existing PostgreSQL instance, set all three variables and omit
`docker compose up`. The selected database must already exist, and the user needs
permission to create the application's tables. The application initializes its
schema on first connection.

The Docker Compose service binds only to localhost and stores data in a named
volume. Stop it with `docker compose down`; this keeps saved runs. Do not remove
the volume unless you intend to erase the local run history.

Environment files and credentials are not committed. Docker Compose can read a
local `.env`, but the Java application reads process environment variables:
export the connection settings in the shell used to launch Gradle.

### HTTP API

| Method and route | Result |
| --- | --- |
| `GET /api/datasets` | Available complete team datasets |
| `GET /api/runs` | Latest 100 saved runs, newest first |
| `GET /api/runs/{id}` | Saved run with team snapshots, or 404 |
| `POST /api/runs` | JSON `{"datasetId":"worlds2024.yml","iterations":10000}`; returns 201 after saving |

API failures return JSON with a `message` field. Database configuration and
connection failures return 503 without leaking connection strings or passwords.
The UI uses same-origin requests; cross-origin writes are rejected.

## Console mode

The original console simulator remains available without PostgreSQL:

```powershell
.\gradlew.bat runCli
```

Console mode uses the settings in `src\main\kotlin\Main.kt` and does not save runs.

## Build and tests

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat test --tests "io.wongaz.model.core.MatchTest"
.\gradlew.bat :web:jsNodeTest
.\gradlew.bat :shared:allTests
.\gradlew.bat integrationTest
.\gradlew.bat test integrationTest
```

`test` runs JVM unit and HTTP API tests without a database or Docker. Browser UI
state tests run on Node.js via `:web:jsNodeTest`; shared JSON contracts are tested
on both JVM and JS via `:shared:allTests`. `integrationTest` runs
against a disposable PostgreSQL container with isolated data and does not use
the application's connection settings or development database. Start Docker
Desktop before running it. Missing Docker is an integration-test failure, not a
silently skipped test. `build` includes JVM, browser-state, and shared-contract
tests plus the production web bundle; integration tests are an
explicit, separate task.

Use `--tests "fully.qualified.TestClass.methodName"` to select a single test with
either task. Reports are under `build\reports\tests\test` and
`build\reports\tests\integrationTest`.

The modules are the root JVM simulator/backend, `web` (Compose HTML browser
client), and `shared` (serializable API DTOs). `:web:jsBrowserDistribution`
builds the frontend alone. The root `processResources` copies that bundle into
classpath `web` resources, so `run` and application distributions serve the same
UI. `installDist` produces runnable server scripts with all dependencies.
