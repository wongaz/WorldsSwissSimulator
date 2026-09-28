# Copilot instructions for WorldsSwissSimulator

## Build and test commands

- Build the project: `.\gradlew.bat build`
- Run the simulator: `.\gradlew.bat run` (application entry point: `io.wongaz.MainKt`)
- Run the test suite: `.\gradlew.bat test`
- Run one test class or method: `.\gradlew.bat test --tests "fully.qualified.TestClass"` or `.\gradlew.bat test --tests "fully.qualified.TestClass.testName"`

Use JDK 25 and set `JAVA_HOME` to its installation directory before running the commands above. The build pins the Kotlin/JVM toolchain to Java 25 and uses the Gradle 9.3.0 wrapper, which supports running on JDK 25.

The build uses Kotlin `2.3.21` and KSP `2.3.12` (KSP2). KSP now versions independently of Kotlin; do not apply the old Kotlin-version-prefix matching rule when updating these plugins.

## High-level architecture

- This is a Kotlin/JVM Gradle project for simulating League of Legends Worlds Swiss-stage outcomes. `Main.kt` loads a YAML resource such as `worlds2025.yml`, parses teams through `TeamLoaderManager`, runs a simulation manager, and prints `SimulationResult` lines.
- Team data lives in YAML under `src/main/resources` with a top-level `teams` list. Jackson YAML plus the Kotlin module deserialize into `LoadedTeams` and `Team`; `category` values map to the `Category` enum.
- `SingleThreadedSimManager` is the active simulation path. For each iteration it deep-copies the original teams, creates a `WorldsSwissFormatSchedulerComponent`, runs a `SwissFormatScheduler`, and counts qualified teams by `teamSignature`. Deep copies are important because `Team` instances carry mutable match history.
- Dependency wiring uses `kotlin-inject` and KSP. `WorldsSwissFormatSchedulerComponent` provides the team list, the default `PureEloSimulation`, the default `NoEloNoRematchRule`, and `endCondition = 3`; callers use the generated `WorldsSwissFormatSchedulerComponent::class.create(...)` function.
- `SwissFormatScheduler` runs `(2 * endCondition) - 1` Swiss rounds. Each round groups teams by current `WinLossRecord`, creates matches within each record pool, uses best-of-one matches normally, and uses `firstTo = 2` for qualification or elimination pools where wins or losses are `endCondition - 1`.
- `Match` simulates immediately in its initializer by repeatedly calling an `IGameSimulation` until one side reaches `firstTo`. The scheduler then records the completed match on both participating teams so future matchmaking rules can inspect prior opponents.
- Matchmaking rules extend `AbstractMatchMakingRule`. The base flow builds a complete `JTournamentGraph`, lets the rule remove forbidden pairings or adjust weights, runs randomized greedy matching, and calls `unblock` until a complete matching is produced. The currently wired rule is `NoEloNoRematchRule`, which only removes rematches.
- `JTournamentGraph` is the active graph implementation backed by JGraphT. The custom `TournamentGraph`, `MultiThreadedSimManager`, detailed simulation manager, domestic-match rules, and several Elo/domestic variants contain `TODO("Not yet implemented")` and are not on the default execution path.

## Key conventions

- Source files are under `src/main/kotlin`, but packages are rooted at `io.wongaz`; the directory layout does not fully mirror package names.
- `teamSignature` is the stable team identity used in result maps, output, `Team.toString()`, and several match comparisons. Preserve uniqueness when adding or loading teams.
- Preserve seed propagation when changing stochastic behavior. The component passes the same `Random` into the game simulation and matchmaking rule, and `JTournamentGraph` passes it to randomized matching.
- Add new match simulations behind `IGameSimulation`; return one of the two input `Team` objects as the winner.
- Add new matchmaking behavior by subclassing `AbstractMatchMakingRule` and implementing `removeMatches`, `updateWeights`, and `unblock`. Avoid over-constraining the graph without a real `unblock` strategy, because `generateMatchPairs` loops until it gets a complete matching.
- Keep match creation centralized through `MatchFactory` so the configured `IGameSimulation` and `firstTo` semantics stay consistent.
- The repository uses Kotlin official code style (`kotlin.code.style=official` in `gradle.properties`).
