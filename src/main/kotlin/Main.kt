package io.wongaz

import io.wongaz.loader.TeamLoaderManager
import io.wongaz.tournamentplanner.SwissFormatScheduler
import kotlin.random.Random

fun main() {
    val stream = object {}.javaClass.getResourceAsStream("/worlds2024.yml")!!
    val teamLoader = TeamLoaderManager()
    val teams = teamLoader.getTeamsFromStream(stream)
//    val graph = JTournamentGraph(teams)
//    graph.runNodeMatching(Random)
    val swissScheduler = SwissFormatScheduler(3, teams, Random)
    swissScheduler.runTournament()
    swissScheduler.getQualifiedTeams().forEachIndexed() { index, team -> println("\t Qualified ${index+1}: ${team.teamSignature}") }
    swissScheduler.getEliminatedTeams().forEachIndexed() { index, team -> println("\t Eliminated ${index+1}: ${team.teamSignature}") }
}