package io.wongaz

import io.wongaz.loader.TeamLoaderManager
import io.wongaz.matchsimulation.PureEloSimulation
import io.wongaz.model.Match
import kotlin.random.Random

fun main() {
    val stream = object {}.javaClass.getResourceAsStream("/worlds2024.yml")!!
    val teamLoader = TeamLoaderManager()
    val teams = teamLoader.getTeamsFromStream(stream)
    val t1  = teams.random()
    val t2 = teams.random()
    val simulation = PureEloSimulation(Random)
    val m = Match(t1, t2, 2, simulation)

    print(m.toString())
}