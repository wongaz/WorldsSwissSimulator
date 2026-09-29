package io.wongaz.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.wongaz.api.PairingDto
import io.wongaz.api.SavedRunDto
import io.wongaz.api.TeamStandingDto
import io.wongaz.api.TournamentDto
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.*

@Composable
internal fun TournamentView(controller: RunController, screen: RunScreen, saved: SavedRunDto) {
    val scope = rememberCoroutineScope()
    Section(attrs = {
        id("tournament-view")
        classes("panel", "tournament-panel")
        attr("aria-labelledby", "pairings-heading")
        attr("data-run-id", saved.summary.id)
    }) {
        Div(attrs = { classes("section-heading", "tournament-heading") }) {
            Div {
                P(attrs = { classes("eyebrow") }) { Text("INDIVIDUAL TOURNAMENTS") }
                H2(attrs = { id("pairings-heading") }) { Text("Swiss round pairings") }
            }
            Button(attrs = {
                id("back-to-simulation")
                classes("button", "secondary")
                if (screen.busy) disabled()
                onClick { controller.showSimulation() }
            }) { Text("Back to simulation") }
        }
        P(attrs = { classes("help") }) {
            Text("${saved.summary.datasetId} | ${grouped(saved.summary.iterations)} iterations | ${saved.summary.completedAt}")
        }
        P(attrs = { classes("run-id") }) { Text("Run ID: ${saved.summary.id}") }
        if (saved.summary.tournamentCount == 0) {
            P(attrs = { classes("help") }) {
                Text("Pairing details were not recorded for this older run. Start a new simulation to explore individual tournaments.")
            }
        } else {
            P(attrs = { classes("help"); id("tournament-help") }) {
                Text("One tournament per page. Group labels are the records before each round; scores are games won in that match.")
            }
            val selectedNumber = screen.tournamentPage
            Nav(attrs = { classes("tournament-pagination"); attr("aria-label", "Tournament pages") }) {
                Button(attrs = {
                    classes("button", "secondary")
                    if (screen.busy || selectedNumber <= 1) disabled()
                    onClick { scope.launch { controller.openTournament(selectedNumber - 1) } }
                }) { Text("Previous") }
                val pages = tournamentPages(selectedNumber, saved.summary.tournamentCount)
                pages.forEachIndexed { index, page ->
                    if (index > 0 && page > pages[index - 1] + 1) {
                        Span(attrs = { classes("page-gap"); attr("aria-hidden", "true") }) { Text("...") }
                    }
                    Button(attrs = {
                        classes("button", "secondary", "page-number")
                        attr("aria-label", "Tournament ${grouped(page)}")
                        if (page == selectedNumber) attr("aria-current", "page")
                        if (screen.busy) disabled()
                        onClick { scope.launch { controller.openTournament(page) } }
                    }) { Text(grouped(page)) }
                }
                Button(attrs = {
                    classes("button", "secondary")
                    if (screen.busy || selectedNumber >= saved.summary.tournamentCount) disabled()
                    onClick { scope.launch { controller.openTournament(selectedNumber + 1) } }
                }) { Text("Next") }
            }
            Div(attrs = { classes("tournament-selector") }) {
                Div {
                    Label(forId = "tournament-number") { Text("Jump to tournament") }
                    Input(type = InputType.Number, attrs = {
                        id("tournament-number")
                        value(screen.tournamentNumberText)
                        attr("min", "1")
                        attr("max", saved.summary.tournamentCount.toString())
                        attr("step", "1")
                        attr("required", "")
                        attr("aria-describedby", "tournament-help")
                        if (screen.busy) disabled()
                        onInput { controller.setTournamentNumber(it.target.value) }
                        onKeyDown {
                            if (it.key == "Enter") scope.launch { controller.openTournament() }
                        }
                    })
                }
                Button(attrs = {
                    attr("type", "button")
                    classes("button", "secondary")
                    if (screen.busy) disabled()
                    onClick { scope.launch { controller.openTournament() } }
                }) { Text("View tournament") }
            }
            screen.tournamentError?.let { message ->
                P(attrs = { classes("status", "error"); attr("role", "alert") }) { Text(message) }
            }
            val tournament = screen.tournament
            if (tournament != null) {
                val names = saved.results.associate { it.teamSignature to it.teamName }
                H3(attrs = { classes("tournament-title"); attr("aria-live", "polite") }) {
                    Text("Tournament ${grouped(tournament.iteration)} of ${grouped(saved.summary.tournamentCount)}")
                }
                key(saved.summary.id, tournament.iteration) {
                    TournamentBracket(tournament, names)
                }
            } else if (screen.busy) {
                P(attrs = { classes("help"); attr("role", "status") }) { Text("Loading tournament details...") }
            }
        }
    }
}

private class TeamPathSelection {
    var hovered: String? by mutableStateOf(null)
    var focused: String? by mutableStateOf(null)
    val active: String? get() = hovered ?: focused
}

@Composable
private fun TournamentBracket(tournament: TournamentDto, names: Map<String, String>) {
    val selection = remember { TeamPathSelection() }
    val active = selection.active
    Div(attrs = { classes("team-path-summary"); attr("aria-live", "polite"); attr("aria-atomic", "true") }) {
        if (active == null) {
            P { Text("Hover or focus a team to trace its entire tournament path. Tap a team on touch screens.") }
        } else {
            P(attrs = { classes("path-heading") }) { Text("${names.getValue(active)} - full tournament path") }
            Ol(attrs = { classes("path-steps") }) {
                tournamentPath(tournament, active).forEach { step ->
                    Li {
                        Text("R${step.round}: ${if (step.won) "W" else "L"} ${step.score}-${step.opponentScore} vs ${names.getValue(step.opponent)} (${step.wins}-${step.losses})")
                    }
                }
            }
            val qualified = tournament.qualified.firstOrNull { it.teamSignature == active }
            val eliminated = tournament.eliminated.firstOrNull { it.teamSignature == active }
            (qualified ?: eliminated)?.let { team ->
                P(attrs = { classes("path-outcome") }) {
                    Text("${if (qualified != null) "Qualified" else "Eliminated"} ${team.wins}-${team.losses}")
                }
            }
        }
    }
    Div(attrs = {
        classes("rounds-scroll")
        attr("tabindex", "0")
        attr("role", "region")
        attr("aria-label", "Swiss round pairings and final results; scroll horizontally to see all columns")
    }) {
        Div(attrs = {
            classes("round-columns")
            if (active != null) classes("tracing-path")
        }) {
            val rounds = tournament.rounds.sortedBy { it.number }
            val finalColumn = (rounds.lastOrNull()?.number ?: 0) + 1
            (rounds.map { it.number } + finalColumn).forEach { column ->
                val label = if (column == finalColumn) "Final results" else "Round $column"
                Section(attrs = {
                    classes("round-column")
                    attr("aria-label", label)
                    attr("data-column", column.toString())
                }) {
                    H3 { Text(label) }
                    Div(attrs = { classes("round-groups") }) {
                        TerminalGroup(
                            "Qualified", "qualified", terminalTeamsForColumn(tournament.qualified, column), names, selection
                        )
                        rounds.firstOrNull { it.number == column }?.groups?.forEach { group ->
                            Section(attrs = {
                                classes("pairing-group")
                                if (group.matches.any { it.team1 == active || it.team2 == active }) classes("path-group")
                                attr("aria-label", "${group.wins}-${group.losses} record")
                                attr("data-record", "${group.wins}-${group.losses}")
                            }) {
                                H4 { Text("${group.wins}-${group.losses}") }
                                group.matches.forEach { match -> PairingCard(match, names, selection) }
                            }
                        }
                        TerminalGroup(
                            "Eliminated", "eliminated", terminalTeamsForColumn(tournament.eliminated, column), names, selection
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingCard(match: PairingDto, names: Map<String, String>, selection: TeamPathSelection) {
    Div(attrs = {
        classes("pairing-card")
        if (selection.active == match.team1 || selection.active == match.team2) classes("path-match")
    }) {
        P(attrs = { classes("match-format") }) { Text("Bo${match.firstTo * 2 - 1}") }
        TeamButton(match.team1, names, selection, match.team1Wins, match.team1Wins > match.team2Wins)
        TeamButton(match.team2, names, selection, match.team2Wins, match.team2Wins > match.team1Wins)
    }
}

@Composable
private fun TeamButton(
    signature: String,
    names: Map<String, String>,
    selection: TeamPathSelection,
    score: Int? = null,
    winner: Boolean = false
) {
    Button(attrs = {
        classes("team-button", "pairing-team")
        attr("type", "button")
        if (winner) classes("match-winner")
        if (selection.active == signature) classes("path-team")
        attr("data-team-signature", signature)
        attr("aria-label", "${names.getValue(signature)}${score?.let { ", $it games" }.orEmpty()}${if (winner) ", winner" else ""}; trace tournament path")
        onMouseEnter { selection.hovered = signature }
        onMouseLeave { selection.hovered = null }
        onFocus {
            selection.hovered = null
            selection.focused = signature
        }
        onBlur { selection.focused = null }
    }) {
        Span(attrs = { classes("pairing-team-name") }) {
            Span(attrs = { classes("pairing-signature") }) { Text(signature) }
            Span(attrs = { classes("pairing-name") }) { Text(names.getValue(signature)) }
        }
        if (score != null) {
            Span(attrs = { classes("match-score") }) { Text(score.toString()) }
        }
    }
}

@Composable
private fun TerminalGroup(
    label: String,
    style: String,
    teams: List<TeamStandingDto>,
    names: Map<String, String>,
    selection: TeamPathSelection
) {
    if (teams.isEmpty()) return
    Section(attrs = {
        classes("terminal-group", style)
        if (teams.any { it.teamSignature == selection.active }) classes("path-group")
        attr("aria-label", label)
    }) {
        H4 { Text("$label (${teams.size})") }
        teams.groupBy { it.wins to it.losses }.toList()
            .sortedWith(compareByDescending<Pair<Pair<Int, Int>, List<TeamStandingDto>>> { it.first.first }
                .thenBy { it.first.second })
            .forEach { (record, group) ->
                Section(attrs = {
                    classes("standing-group")
                    attr("aria-label", "${record.first}-${record.second} record")
                }) {
                    H5 { Text("${record.first}-${record.second}") }
                    Ul {
                        group.forEach { team ->
                            Li {
                                TeamButton(team.teamSignature, names, selection)
                            }
                        }
                    }
                }
            }
    }
}

internal fun terminalTeamsForColumn(teams: List<TeamStandingDto>, column: Int): List<TeamStandingDto> =
    teams.filter { it.wins + it.losses == column - 1 }

internal data class TeamPathStep(
    val round: Int,
    val opponent: String,
    val score: Int,
    val opponentScore: Int,
    val wins: Int,
    val losses: Int
) {
    val won: Boolean get() = score > opponentScore
}

internal fun tournamentPath(tournament: TournamentDto, signature: String): List<TeamPathStep> =
    tournament.rounds.sortedBy { it.number }.flatMap { round ->
        round.groups.flatMap { group ->
            group.matches.filter { it.team1 == signature || it.team2 == signature }.map { match ->
                val first = match.team1 == signature
                val score = if (first) match.team1Wins else match.team2Wins
                val opponentScore = if (first) match.team2Wins else match.team1Wins
                TeamPathStep(
                    round.number, if (first) match.team2 else match.team1, score, opponentScore,
                    group.wins + if (score > opponentScore) 1 else 0,
                    group.losses + if (score < opponentScore) 1 else 0
                )
            }
        }
    }
