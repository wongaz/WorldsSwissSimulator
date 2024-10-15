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
    val rSeed = System.currentTimeMillis()
    println("Seed $rSeed")
//    val randomSeed = Random(rSeed)
    val randomSeed = Random(1729013983773)
    val swissScheduler = SwissFormatScheduler(3, teams, randomSeed)
    swissScheduler.runTournament()
    swissScheduler.getQualifiedTeams().forEachIndexed() { index, team -> println("\t Qualified ${index+1}: ${team.teamSignature}") }
    swissScheduler.getEliminatedTeams().forEachIndexed() { index, team -> println("\t Eliminated ${index+1}: ${team.teamSignature}") }
}