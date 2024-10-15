package io.wongaz

import io.wongaz.loader.TeamLoaderManager
import io.wongaz.matchsimulation.PureEloSimulation
import io.wongaz.model.core.Match
import io.wongaz.tournamentplanner.matchmaking.graph.JTournamentGraph
import kotlin.random.Random

fun main() {
    val stream = object {}.javaClass.getResourceAsStream("/worlds2024.yml")!!
    val teamLoader = TeamLoaderManager()
    val teams = teamLoader.getTeamsFromStream(stream)
    val graph = JTournamentGraph(teams)
    graph.runNodeMatching(Random)
}