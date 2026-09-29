# Copilot Instructions

Monte-Carlo simulator for the League of Legends Worlds Swiss-stage format. Kotlin/JVM Ktor backend, Kotlin/JS Compose for Web (HTML DOM) client, PostgreSQL via JDBC, Gradle Kotlin DSL, kotlin-inject (KSP) for DI, JGraphT for matchmaking, and Jackson YAML for input.

## Commands

- Build: `.\gradlew.bat build`
- Web app: `.\gradlew.bat run` (`io.wongaz.ServerMainKt`), then open `http://localhost:8080`. Set `SIM_DB_PASSWORD` and start the local database with `docker compose up -d --wait`; see `README.md` for connection settings. `SIM_HTTP_PORT` changes the port.
- Data exploration: `.\scripts\Initialize-SupersetEnv.ps1`, then `docker compose --profile analytics up -d --build --wait`. Superset runs at `http://localhost:8088` with the preconfigured read-only `Worlds Swiss` connection; credentials stay in ignored `.env`.
- Original console simulation without persistence: `.\gradlew.bat runCli` (`io.wongaz.MainKt`).
- Unit tests: `.\gradlew.bat test` (JUnit 5 and Kotlin test, no Docker). Single class or method: `.\gradlew.bat test --tests "fully.qualified.ClassName"` or `.\gradlew.bat test --tests "fully.qualified.ClassName.methodName"`.
- Browser state tests: `.\gradlew.bat :web:jsNodeTest`; shared JSON contract tests: `.\gradlew.bat :shared:allTests`. Gradle downloads Node/Yarn. These are included in `check`/`build`.
- PostgreSQL integration tests: `.\gradlew.bat integrationTest`; use the same `--tests` selectors. This separate source set uses Testcontainers and requires Docker; it does not use the development database. `build` runs unit tests, not integration tests.
- KSP runs as part of `build`/`compileKotlin`; if generated `create` extension functions go missing from imports (e.g. `io.wongaz.tournamentplanner.create`), do a clean build: `.\gradlew.bat clean build`.

Use JDK 25 and set `JAVA_HOME` to its installation directory before running these commands. The Kotlin/JVM toolchain targets Java 25, and the Gradle 9.3.0 wrapper supports running on JDK 25.

The build uses Kotlin `2.3.21`, KSP `2.3.12` (KSP2), and kotlin-inject `0.9.0`. KSP versions independently of Kotlin; do not apply the old Kotlin-version-prefix matching rule when updating these plugins.

The root `application` plugin owns the backend `run` task; console mode is the
separate `runCli` JavaExec task. `processResources` depends on `bundleWeb`, which
copies `:web:jsBrowserDistribution` into generated classpath `web` resources.
Do not hand-edit generated bundles. The `web` Compose compiler plugin version
matches Kotlin. Keep the generated Yarn lockfile for repeatable frontend builds.

## Architecture

The root JVM project contains the backend and simulator. `web` contains the
Compose HTML client; `shared` contains serializable API DTOs compiled for JVM
and JS. API IDs and timestamps are strings; JDBC and `java.time` types stay on
the server. The browser fetches same-origin `/api` JSON endpoints and never
connects to PostgreSQL directly.

`ServerMain` wires `DefaultRunService` to `PostgresRunRepository`.
Ktor handlers call blocking `RunService` methods on a background dispatcher.
Only one simulation request runs at a time; competing launches return 409.
The server binds to loopback, not a public interface, and rejects cross-origin
writes. Preserve these defaults unless adding authentication and deployment
controls deliberately.
`DefaultRunService` validates the selected dataset and iteration count, runs the
simulator, snapshots team metadata and results, and returns only after saving.
PostgreSQL stores completed runs, not in-flight or failed attempts. Run metadata
and all team results must be saved atomically; database errors must remain visible
to the UI rather than switching to an in-memory fallback. The repository is
initialized lazily so the browser can load and show configuration/connection
errors. Configuration is read from `SIM_DB_URL`, `SIM_DB_USER`, and required
`SIM_DB_PASSWORD`, never a committed credential. Process environment variables
override the local working-directory `.env`; `ServerMain` uses
`DatabaseConfig.fromLocalEnvironment()` lazily. Launch from the repository root
to share Docker Compose's `.env`. `SIM_HTTP_PORT` remains process-environment-only.

The optional Compose `analytics` profile initializes a dedicated PostgreSQL
`superset_reader` role with SELECT grants on current/future simulator tables.
Superset's local-only SQLite metadata lives in its own `superset_data` volume;
do not mix charts/users with simulator tables or delete volumes during setup.

The dataset registry lives in `DefaultRunService`. Add only complete
16-team datasets with unique signatures; `basic_worlds.yml` is a single-team
loader fixture. Historical results contain team snapshots and must not be
reconstructed from the current YAML files.

Top-level dataflow for one simulation run (see `Main.kt`):

1. `TeamLoaderManager.getTeamsFromStream` parses a YAML resource (e.g. `src/main/resources/worlds2024.yml`) into `List<Team>` via Jackson (`jackson-dataformat-yaml` + `jackson-module-kotlin`). Note: `kotlinx-serialization-json` is on the classpath and `Team` is `@Serializable`, but loading actually goes through Jackson — keep both annotations working if you change the model.
2. An `AbstractSimManager` runs `iterations` independent tournaments. `Main.kt` uses `MultiThreadedSimManager`, which partitions iterations across coroutine workers on `Dispatchers.Default`, collects qualification counts in worker-local maps, then combines them via `mergeResults`. Keep shared result updates outside the parallel workers. `SingleThreadedSimManager` is also implemented. Each iteration starts from `deepCopyTeams()` because `Team` holds mutable match history.
3. Per iteration, the manager instantiates a kotlin-inject component: `WorldsSwissFormatSchedulerComponent::class.create(copy).swissFormatScheduler`. Its defaults are `PureEloSimulation` and `NoEloNoRematchRule`. The `create` extension is generated by KSP from the `@Component` annotation — do not write it by hand.
4. `SwissFormatScheduler.runTournament` simulates rounds 1..(2*endCondition − 1). `endCondition` defaults to 3 (3 wins = qualify, 3 losses = eliminate). For each round it iterates every possible W-L bucket, filters teams via `Team.equalsWinLoss`, and asks the injected `AbstractMatchMakingRule` to pair them. When `wins == endCondition - 1` or `losses == endCondition - 1` it requests Bo3 matches by passing `fto = 2` (otherwise Bo1, `fto = 1`).
5. `AbstractSimManager.simResults` is keyed by `teamSignature`; the manager increments qualification counts via `updateResults`, and `SimulationResult.makeSimpleResultsLine` produces the output strings printed by `main`.

### Matchmaking pipeline (`tournamentplanner.matchmaking`)

`AbstractMatchMakingRule.generateMatchPairs` is the template method every rule plugs into:

1. Build a complete `ITournamentGraph` over the bucket's teams.
2. `removeMatches` — subclass strips disallowed edges (e.g. `removeRematches` uses `Team.getPreviousPlayedTeams()`).
3. `updateWeights` — subclass sets edge weights (used by Elo-aware variants).
4. `runNodeMatching(seed)` — runs `RandomizedGreedyMaximumCardinalityMatching` (a Kotlin port of JGraphT's greedy max-cardinality matcher, modified to shuffle vertices/edges with an injectable `Random` so seeded runs are reproducible).
5. If the matching is incomplete (`size != teams.size / 2`), `unblock` is called to relax constraints, then matching re-runs in a loop.
6. Successful pairs are turned into `Match` objects through the injected `MatchFactory`.

The matching loop has no retry limit. Rules that can make a complete matching impossible need an `unblock` strategy rather than a no-op.

There are **two** `ITournamentGraph` implementations: `JTournamentGraph` (JGraphT-backed, in use) and `TournamentGraph` (a hand-rolled adjacency map, mostly `TODO`). New rules should use `JTournamentGraph` via the interface unless you are deliberately finishing the hand-rolled one.

### Game simulation (`matchsimulation`)

`IGameSimulation.runSingleGameSimulation(team1, team2): Team` returns one of the two input teams as the winner of a single game. `Match` simulates immediately in its initializer; `simulateMatch` loops until one side reaches `firstTo`. The scheduler records the completed match on both teams. Implementations live in `matchsimulation/rules/` (`PureEloSimulation` uses the standard Elo expected-score formula with a seeded `Random`; others bias by side/region/category). Use the `IGameSimulation` interface — do not instantiate concrete simulations from inside rules or schedulers; let kotlin-inject provide them.

### Dependency injection (kotlin-inject)

- Constructor-injected classes are annotated `@Inject` (e.g. `SwissFormatScheduler`, `MatchFactory`, `PureEloSimulation`, rules).
- Composition roots are `@Component` abstract classes (e.g. `WorldsSwissFormatSchedulerComponent`, `NoEloNoRematchComponent`). Bindings come from constructor params marked `@get:Provides` and from `@Provides` methods (e.g. `endCondition() = 3`).
- KSP generates a `create` extension on the component's `KClass`: call as `MyComponent::class.create(args)`. After adding/removing a `@Component` or a binding, recompile so the generated code stays in sync.
- To swap matchmaking rules or sims globally, change the defaults on `WorldsSwissFormatSchedulerComponent`'s constructor rather than editing `SwissFormatScheduler`.

## Conventions

- Package root is `io.wongaz`. Source layout mirrors domains: `model/core` (`Team`, `Match`, `Round`, `Pool`, `WinLossRecord`, `Category`), `model/simulation`, `model/loader`, `tournamentplanner/{scheduler,matchmaking}`, `matchsimulation`, `simulationmanager`, `loader`.
- Preserve unique `teamSignature` values (e.g. `"HLE"`, `"T1"`): result maps and several match comparisons use them as identity. `Team.hasPlayed` uses data-class equality, which compares constructor fields rather than match history.
- `Team` is a `data class` but is **mutated** during a tournament (`addMatch` appends to `winningMatches`/`lossMatches`). Always start a run from `deepCopyTeams()` (`.copy()` per team) so iterations don't leak state.
- Preserve explicit `kotlin.random.Random` propagation through the component, simulation, rule, and graph matcher. The default component shares its `Random` between simulation and matchmaking. The current managers use `Random.Default`; deterministic parallel runs would need explicit per-worker or per-iteration seeds.
- Bo-format is encoded as `firstTo` on `Match` (Bo1 = `firstTo = 1`, Bo3 = `firstTo = 2`, Bo5 = `firstTo = 3`). The scheduler currently uses Bo1 for opening rounds and Bo3 for qualification/elimination rounds.
- YAML team files live in `src/main/resources/` and are loaded by classpath path (`getResourceAsStream("/worlds2024.yml")`). New scenarios should follow the same `teams:` schema as `worlds2024.yml` and `basic_worlds.yml`. `Category` must be one of `EASTERN`, `WESTERN`, `WILDCARD`.
- Several classes are stubs with `TODO("Not yet implemented")`: `TournamentGraph.runNodeMatching`/`removeNode`/`exportGraph`, `NoDomesticNoRematchesRule.*`, and several other rule variants. Treat these as work-in-progress; the default path uses `JTournamentGraph` and `NoEloNoRematchRule`.
- The repository uses Kotlin official code style (`kotlin.code.style=official` in `gradle.properties`); source directories omit the `io.wongaz` package prefix.
