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

## IntelliJ IDEA setup

Open the repository root as a Gradle project using an IntelliJ IDEA version that
supports Java 25 and the project's Kotlin version.
In **File > Project Structure > Project**, select an installed **JDK 25** as the
Project SDK and use the SDK's default language level.

In **Settings > Build, Execution, Deployment > Build Tools > Gradle**, use the
repository's **Gradle wrapper**, select **JDK 25** as the **Gradle JVM**, and set
both build/run and test execution to **Gradle**. Reload all Gradle projects to
import the root backend, `shared`, and `web` modules and regenerate IDE metadata.
An **Invalid Gradle JDK configuration** error usually means the selected SDK is
missing; reselect an installed JDK 25 rather than downloading an older JDK.
`JAVA_HOME` controls terminal builds but does not override an explicitly selected
IntelliJ SDK. Local `.idea` settings are ignored by Git.

Use the Gradle `run` task for the web application or `runCli` for console mode.
Set `SIM_DB_PASSWORD` in the web application's Gradle run configuration;
IntelliJ does not automatically load the repository's `.env` file.

## Run the web application

From PowerShell in the repository root:

```powershell
# If SIM_DB_PASSWORD is not already in your local .env:
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

New runs also retain **every individual tournament**, not a rerun or a sample.
Select **Browse tournaments** from a saved snapshot to open a separate full-width
viewer, away from the simulation controls and aggregate results. The viewer starts
at tournament 1 and displays one tournament per page; use the numbered pages,
Previous/Next, or jump to any tournament number. **Back to simulation** returns to
the same saved results, and reopening the viewer preserves the selected page.
Each round has its own column with boxed pre-round records (0-0, 1-0, 0-1, and so
on). The round contents are vertically centered, expanding toward round 3 and
contracting toward the final round. Match cards show Bo1/Bo3 and game scores,
with winners highlighted in green rather than labeled. Qualified teams sit above
the active matches and eliminated teams below them in the next round's column:
3-0 / 0-3 in Round 4, then 3-1 / 1-3 in Round 5. A final results column after
Round 5 contains the 3-2 and 2-3 groups.
Hover or keyboard-focus any team (or tap on touch screens) to highlight its entire
tournament path across matches and final placement. The path summary lists each
opponent, score, resulting record, and final outcome. On smaller screens, scroll
the round columns horizontally.

Tournament details are fetched only when opening or paging through the viewer,
one selected tournament at a time. The backend streams
details through a temporary file and bounded database batches rather than
keeping all iterations in memory. Retaining every tournament increases run
time and storage substantially: a million-iteration run can require multiple
gigabytes of temporary disk and database space. Start with a small iteration
count for investigations. Temporary files are removed on normal completion or
failure; a forced process termination can leave `swiss-tournaments-*.jsonl` in
the JVM's temporary directory. Older saved runs retain their aggregate results
but have no pairing details; these cannot be reconstructed from the counts.

Simulation and database work run on the Kotlin backend. A run is shown as saved only
after its metadata, all team results, and every tournament's details have been
committed to PostgreSQL in one transaction.
Database failures are displayed in the UI rather than falling back to temporary
in-memory history. If the database is unavailable, start it and refresh. Missing
configuration requires setting `SIM_DB_PASSWORD` in the local `.env` or in the
server environment. Restart the backend after changing process environment variables.

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
Existing installations are upgraded in place with a `tournament_count` column
and a `tournament_results` table containing one indexed JSON snapshot per
run/iteration; the database user also needs permission to alter its own tables.

The Docker Compose service binds only to localhost and stores data in a named
volume. Stop it with `docker compose down`; this keeps saved runs. Do not remove
the volume unless you intend to erase the local run history.

Environment files and credentials are not committed. Both Docker Compose and
the backend can read a local `.env`. For database settings, the backend uses
process environment variables first, then `.env` in its working directory,
then the URL/username defaults above. Passwords still have no default.
Launch Gradle from the repository root; when running `ServerMain` in IntelliJ,
set the run configuration's working directory to the repository root.
Use plain or double-quoted values and comments in `.env`; use literal values rather
than Docker Compose variable interpolation (`${...}`). A missing file is fine
when environment variables supply the settings, but a malformed or unreadable
file is reported as a configuration error. `SIM_HTTP_PORT` remains a process
environment setting.

### Explore PostgreSQL with Apache Superset

The optional `analytics` Compose profile provides Superset at
**http://localhost:8088**, already connected to the `worlds_swiss` database:

```powershell
.\scripts\Initialize-SupersetEnv.ps1
docker compose --profile analytics up -d --build --wait
```

Log in with username **`admin`** and the **`SUPERSET_ADMIN_PASSWORD`** value in
your local `.env`. The setup script generates random Superset credentials without
printing them or changing existing values. Keep `.env` private and retain
`SUPERSET_SECRET_KEY`: it encrypts Superset's saved connection credentials.
The simulator's existing `SIM_DB_PASSWORD` must match the initialized PostgreSQL
volume. The PostgreSQL user must be able to create roles and grant access
(the local Compose default user can).

In **SQL > SQL Lab**, select the **Worlds Swiss** database and **public** schema.
The connection uses the Docker service name `postgres`, not `localhost`, and a
dedicated `superset_reader` role with SELECT-only table permissions. New simulator
tables created by `SIM_DB_USER` also receive read access. The normal simulator
account and existing run data are unchanged.

Initialization registers `simulation_runs` and `team_run_results` as datasets if
the simulator has already created them. Otherwise start the simulator and load
history once to initialize its schema, then add the tables through
**Data > Datasets > + Dataset**. Save SQL Lab queries as virtual datasets to build
charts. For example:

```sql
SELECT
    r.id AS run_id,
    r.dataset_id,
    r.completed_at,
    r.iterations,
    t.team_signature,
    t.team_name,
    t.region,
    t.elo,
    t.qualifications,
    100.0 * t.qualifications / r.iterations AS qualification_percent
FROM public.simulation_runs AS r
JOIN public.team_run_results AS t ON t.run_id = r.id
ORDER BY r.completed_at DESC, qualification_percent DESC;
```

Superset's users, connections, charts, and dashboards are stored separately in
the `superset_data` volume using SQLite, suitable for this single-user local
setup only. Simulation results remain in PostgreSQL's `postgres_data` volume.
Superset is bound to localhost and uses HTTP; do not expose this development
configuration publicly. A production deployment needs HTTPS, a production
metadata database, and deliberate authentication/deployment configuration.

Superset initialization is repeatable and preserves existing administrators and
charts. Changing `SUPERSET_ADMIN_PASSWORD` in `.env` does not reset an existing
administrator; change it through Superset's account settings.

Stop just Superset with `docker compose --profile analytics stop superset`.
Plain `docker compose up -d --wait` still starts only PostgreSQL. The first
analytics build downloads the larger Superset image and installs its PostgreSQL
driver; subsequent starts reuse the image and saved configuration.

### HTTP API

| Method and route | Result |
| --- | --- |
| `GET /api/datasets` | Available complete team datasets |
| `GET /api/runs` | Latest 100 saved runs, newest first |
| `GET /api/runs/{id}` | Saved run with team snapshots, or 404 |
| `GET /api/runs/{id}/tournaments/{iteration}` | One saved tournament's rounds, pairings, scores and final standings (1-based); 404 if not recorded |
| `POST /api/runs` | JSON `{"datasetId":"worlds2024.yml","iterations":10000}`; returns 201 after saving |

API failures return JSON with a `message` field. Database configuration and
connection failures return 503 without leaking connection strings or passwords.
The UI uses same-origin requests; cross-origin writes are rejected.
Run summaries include `tournamentCount` (0 for legacy runs, otherwise equal to
`iterations`). Tournament match and standing identifiers refer to the saved
run's team snapshots, never the current dataset.

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
