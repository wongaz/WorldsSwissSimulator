package io.wongaz.tournamentplanner.matchmaking.graph

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.matching.RandomizedGreedyMaximumCardinalityMatching
import org.jgrapht.alg.interfaces.MatchingAlgorithm
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import kotlin.random.Random

class JTournamentGraph(val teams: List<Team>, private val seed: Random) {
    val graph = SimpleWeightedGraph<Team, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)

    init {
        makeCompleteGraph(teams)
    }

    private fun makeCompleteGraph(teams: List<Team>){
        for (team in teams) {
            this.graph.addVertex(team)
        }

        for((index, team1) in teams.withIndex()){
            for(k in index+1..<teams.size){
                val team2 = teams[k]
                this.graph.addEdge(team1, team2)
            }
        }
    }

    fun runNodeMatching(randomSeed: Random){
        val matchingAlgorithm: MatchingAlgorithm<Team, DefaultWeightedEdge> =
            RandomizedGreedyMaximumCardinalityMatching(this.graph, seed = this.seed)
        val output = matchingAlgorithm.matching
        for(edge in output.edges){
            val src = output.graph.getEdgeSource(edge) as Team
            val end = output.graph.getEdgeTarget(edge) as Team
            println("${src.teamSignature} - ${end.teamSignature}")
        }
    }
}