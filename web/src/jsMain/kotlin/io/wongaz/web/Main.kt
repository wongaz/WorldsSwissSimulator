package io.wongaz.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.wongaz.api.SavedRunDto
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import kotlin.math.roundToInt

fun main() {
    renderComposable(rootElementId = "root") {
        val controller = remember { RunController(BrowserRunApi()) }
        val screen by controller.screen.collectAsState()
        LaunchedEffect(controller) { controller.initialize() }
        Simulator(controller, screen)
    }
}

@Composable
private fun Simulator(controller: RunController, screen: RunScreen) {
    val scope = rememberCoroutineScope()
    Main(attrs = { classes("app-shell") }) {
        Header(attrs = { classes("hero") }) {
            Div {
                P(attrs = { classes("eyebrow") }) { Text("LEAGUE OF LEGENDS · WORLDS") }
                H1 { Text("Swiss stage simulator") }
                P(attrs = { classes("hero-copy") }) {
                    Text("Explore qualification odds. Every completed run is a saved, reproducible snapshot.")
                }
            }
            Span(attrs = { classes("hero-badge") }) { Text("Kotlin · Compose for Web") }
        }
        val selectedRun = screen.selectedRun
        if (screen.view == RunView.TOURNAMENT && selectedRun != null) {
            TournamentView(controller, screen, selectedRun)
        } else Div(attrs = { classes("workspace") }) {
            Aside(attrs = { classes("sidebar"); attr("aria-label", "Simulation controls and saved history") }) {
                Section(attrs = { classes("panel", "controls"); attr("aria-labelledby", "configure-heading") }) {
                    P(attrs = { classes("eyebrow") }) { Text("NEW SIMULATION") }
                    H2(attrs = { id("configure-heading") }) { Text("Configure your run") }
                    Label(forId = "dataset") { Text("Dataset") }
                    Select(attrs = {
                        id("dataset")
                        if (screen.busy || screen.datasets.isEmpty()) disabled()
                        onChange { controller.selectDataset(it.target.value) }
                    }) {
                        if (screen.datasets.isEmpty()) {
                            Option(value = "") { Text("No dataset loaded") }
                        }
                        screen.datasets.forEach { dataset ->
                            Option(value = dataset.id, attrs = {
                                if (dataset.id == screen.datasetId) attr("selected", "selected")
                            }) { Text(dataset.label) }
                        }
                    }
                    Label(forId = "iterations") { Text("Iterations") }
                    Input(type = InputType.Number, attrs = {
                        id("iterations")
                        value(screen.iterationsText)
                        attr("min", "1")
                        attr("max", "1000000")
                        attr("step", "1")
                        attr("required", "")
                        attr("aria-describedby", "iteration-help")
                        if (screen.busy) disabled()
                        onInput { controller.setIterations(it.target.value) }
                    })
                    P(attrs = { id("iteration-help"); classes("help") }) {
                        Text("1–1,000,000 simulations. More iterations give steadier estimates and take longer.")
                    }
                    P(attrs = { classes("help") }) {
                        Text("Every tournament's pairings and scores are saved. Large runs require substantial disk space and database storage; start small when exploring.")
                    }
                    Button(attrs = {
                        id("run-simulation")
                        classes("button", "primary")
                        if (screen.busy || screen.datasets.isEmpty()) disabled()
                        onClick { scope.launch { controller.startRun() } }
                    }) { Text(if (screen.activity == Activity.RUNNING) "Simulation in progress…" else "Run & save simulation") }
                    P(attrs = { classes("help", "privacy-note") }) {
                        Text("Runs execute on the Kotlin server and are saved in its database. No database credentials are sent to your browser.")
                    }
                }
                Section(attrs = { classes("panel", "history"); attr("aria-labelledby", "history-heading") }) {
                    Div(attrs = { classes("section-heading") }) {
                        Div {
                            P(attrs = { classes("eyebrow") }) { Text("DATABASE") }
                            H2(attrs = { id("history-heading") }) { Text("Saved runs") }
                        }
                        Button(attrs = {
                            id("refresh-history")
                            classes("button", "secondary", "compact")
                            attr("aria-label", "Refresh datasets and saved run history")
                            if (screen.busy) disabled()
                            onClick { scope.launch { controller.refresh() } }
                        }) { Text("Refresh") }
                    }
                    P(attrs = { classes("help") }) { Text("The 100 most recent saved runs. Select a snapshot to inspect it.") }
                    if (screen.history.isEmpty()) {
                        P(attrs = { classes("empty-history") }) {
                            Text(if (screen.busy) "Loading saved runs…" else "No saved history loaded. Run a simulation or refresh to check the database.")
                        }
                    } else {
                        Ul(attrs = { id("saved-history"); classes("history-list") }) {
                            screen.history.forEach { run ->
                                Li {
                                    Button(attrs = {
                                        classes("history-item")
                                        attr("data-run-id", run.id)
                                        attr("aria-pressed", (screen.selectedRun?.summary?.id == run.id).toString())
                                        if (screen.busy) disabled()
                                        onClick { scope.launch { controller.openRun(run.id) } }
                                    }) {
                                        Strong { Text(datasetLabel(run.datasetId, screen)) }
                                        Span(attrs = { classes("history-detail") }) {
                                            Text("${grouped(run.iterations)} iterations")
                                        }
                                        Span(attrs = { classes("timestamp") }) { Text(run.completedAt) }
                                        Span(attrs = { classes("run-id") }) { Text(run.id) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Div(attrs = { classes("results-column") }) {
                Div(attrs = { attr("aria-live", "polite"); attr("aria-atomic", "true") }) {
                    if (screen.busy) {
                        Div(attrs = { classes("status", "working"); attr("role", "status") }) {
                            Strong {
                                Text(when (screen.activity) {
                                    Activity.RUNNING -> "Simulating and saving…"
                                    Activity.OPENING -> "Opening saved snapshot…"
                                    Activity.REFRESHING -> "Refreshing datasets and history…"
                                    Activity.TOURNAMENT -> "Loading tournament pairings…"
                                    else -> "Loading datasets and history…"
                                })
                            }
                            if (screen.activity == Activity.RUNNING) {
                                P { Text("Leave this tab open. The results appear only after the server finishes and saves the run. Only one simulation can run on the server at a time.") }
                            }
                            Div(attrs = {
                                id("request-progress")
                                classes("progress-track")
                                attr("role", "progressbar")
                                attr("aria-label", "Request in progress")
                                attr("aria-valuetext", "Working; completion time is unknown")
                            }) { Span {} }
                        }
                    }
                    screen.notice?.let { message ->
                        Div(attrs = { classes("status", "success"); attr("role", "status") }) { Text(message) }
                    }
                }
                screen.error?.let { message ->
                    Div(attrs = { classes("status", "error"); attr("role", "alert") }) {
                        Strong { Text("Request not completed") }
                        P { Text(message) }
                        P(attrs = { classes("help") }) {
                            Text("Existing snapshots are unchanged. Use Refresh to check saved history. Database configuration is managed on the server.")
                        }
                    }
                }
                val saved = screen.selectedRun
                if (saved == null) {
                    Section(attrs = { classes("panel", "empty-results"); attr("aria-labelledby", "results-heading") }) {
                        Div(attrs = { classes("empty-icon"); attr("aria-hidden", "true") }) { Text("↗") }
                        P(attrs = { classes("eyebrow") }) { Text("QUALIFICATION OUTLOOK") }
                        H2(attrs = { id("results-heading") }) { Text("A clearer view of the Swiss stage") }
                        P { Text("Choose a dataset and run a simulation, or open a saved run. Team qualification counts and percentages will appear here.") }
                        Div(attrs = { classes("empty-features") }) {
                            Span { Text("Team-level results") }
                            Span { Text("Elo & seed snapshots") }
                            Span { Text("Database-backed history") }
                        }
                    }
                } else {
                    Results(saved, screen, onBrowseTournaments = {
                        scope.launch { controller.showTournamentViewer() }
                    })
                }
            }
        }
        Footer { Text("Worlds Swiss Simulator · Saved results reflect the dataset and ratings used at the time of each run.") }
    }
}

@Composable
private fun Results(saved: SavedRunDto, screen: RunScreen, onBrowseTournaments: () -> Unit) {
    Section(attrs = {
        id("saved-snapshot")
        classes("panel", "results")
        attr("data-run-id", saved.summary.id)
        attr("aria-labelledby", "results-heading")
    }) {
        Div(attrs = { classes("results-header") }) {
            Div {
                P(attrs = { classes("eyebrow") }) { Text("SAVED SNAPSHOT") }
                H2(attrs = { id("results-heading") }) { Text(datasetLabel(saved.summary.datasetId, screen)) }
            }
            Div(attrs = { classes("snapshot-actions") }) {
                Span(attrs = { classes("saved-badge") }) { Text("Saved to database") }
                Button(attrs = {
                    id("open-tournaments")
                    classes("button", "secondary")
                    if (screen.busy) disabled()
                    onClick { onBrowseTournaments() }
                }) { Text("Browse tournaments") }
            }
        }
        Div(attrs = { classes("metrics") }) {
            Metric("Iterations", grouped(saved.summary.iterations))
            Metric("Teams", saved.results.size.toString())
            Metric("Dataset", saved.summary.datasetId)
        }
        Div(attrs = { classes("snapshot-meta") }) {
            P { Strong { Text("Started: ") }; Text(saved.summary.startedAt) }
            P { Strong { Text("Completed: ") }; Text(saved.summary.completedAt) }
            P(attrs = { classes("snapshot-id") }) { Strong { Text("Run ID: ") }; Text(saved.summary.id) }
        }
        P(attrs = { classes("table-note"); id("table-description") }) {
            Text("Qualification frequency across ${grouped(saved.summary.iterations)} iterations, highest first. Ratings, categories and seeds are the saved values for this run.")
        }
        Div(attrs = { classes("table-scroll"); attr("tabindex", "0"); attr("role", "region"); attr("aria-label", "Team qualification results; scroll horizontally on small screens") }) {
            Table(attrs = {
                id("run-results")
                attr("data-run-id", saved.summary.id)
                attr("aria-describedby", "table-description")
            }) {
                Thead {
                    Tr {
                        listOf("Team", "Region", "Category", "Seed", "Elo", "Qualified", "Qualification %").forEach {
                            Th(attrs = { attr("scope", "col") }) { Text(it) }
                        }
                    }
                }
                Tbody {
                    saved.results.sortedWith(compareByDescending<io.wongaz.api.TeamResultDto> { it.qualifications }.thenBy { it.teamName }).forEach { team ->
                        Tr(attrs = { attr("data-team-signature", team.teamSignature) }) {
                            Th(attrs = { attr("scope", "row") }) {
                                Strong { Text(team.teamName) }
                                Span(attrs = { classes("team-signature") }) { Text(team.teamSignature) }
                            }
                            Td { Text(team.region) }
                            Td { Span(attrs = { classes("category") }) { Text(team.category) } }
                            Td(attrs = { classes("numeric") }) { Text(team.seed.toString()) }
                            Td(attrs = { classes("numeric") }) { Text(grouped(team.elo)) }
                            Td(attrs = { classes("numeric") }) { Text(grouped(team.qualifications)) }
                            Td(attrs = { classes("numeric", "percentage") }) {
                                Text(percentage(team.qualifications, saved.summary.iterations))
                            }
                        }
                    }
                }
            }
        }
        if (saved.results.isEmpty()) {
            P(attrs = { classes("empty-history") }) { Text("This saved run has no team results.") }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Div(attrs = { classes("metric") }) {
        Span { Text(label) }
        Strong { Text(value) }
    }
}

@Composable
private fun Strong(content: @Composable () -> Unit) {
    TagElement<org.w3c.dom.HTMLElement>("strong", null) { content() }
}

private fun datasetLabel(id: String, screen: RunScreen): String =
    screen.datasets.firstOrNull { it.id == id }?.label ?: id

internal fun grouped(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed()

internal fun percentage(count: Int, iterations: Int): String {
    if (iterations <= 0) return "—"
    val hundredths = (count * 10000.0 / iterations).roundToInt()
    return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}%"
}
