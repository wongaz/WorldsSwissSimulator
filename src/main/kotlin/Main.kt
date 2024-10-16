package io.wongaz

import io.wongaz.loader.TeamLoaderManager
import io.wongaz.tournamentplanner.SwissFormatScheduler
import io.wongaz.tournamentplanner.WorldsSwissFormatSchedulerComponent
import io.wongaz.tournamentplanner.create
import io.wongaz.tournamentplanner.matchmaking.di.NoEloNoRematchComponent
import io.wongaz.tournamentplanner.matchmaking.di.create
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import kotlin.random.Random

fun main() {
    val stream = object {}.javaClass.getResourceAsStream("/worlds2024.yml")!!
    val teams = TeamLoaderManager().getTeamsFromStream(stream)
//    val graph = JTournamentGraph(teams)
//    graph.runNodeMatching(Random)
    val rSeed = System.currentTimeMillis()
    println("Seed $rSeed")
//    val randomSeed = Random(rSeed)
    val randomSeed = Random(1729016541378)
    val component = NoEloNoRematchComponent::class.create(randomSeed)
    val worldsSwissFormatSchedulerComponent =
        WorldsSwissFormatSchedulerComponent::class.create(
            teams = teams,
            matchMakingRule = component.matchMakingRule as AbstractMatchMakingRule
        )
//    swissScheduler.runTournament()
//    swissScheduler.getQualifiedTeams().sortedBy { it.teamSignature }.forEachIndexed() { index, team -> println("\t Qualified ${index+1}: ${team.teamSignature}") }
//    println("------------------------------------------")
//    swissScheduler.getEliminatedTeams().sortedBy { it.teamSignature }.forEachIndexed() { index, team -> println("\t Eliminated ${index+1}: ${team.teamSignature}") }
}
