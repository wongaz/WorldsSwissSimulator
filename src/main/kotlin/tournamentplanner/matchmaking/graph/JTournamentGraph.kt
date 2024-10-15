package io.wongaz.tournamentplanner.matchmaking.graph

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.interfaces.ITournamentGraph
import io.wongaz.tournamentplanner.matchmaking.graph.matching.RandomizedGreedyMaximumCardinalityMatching
import org.jgrapht.Graph
import org.jgrapht.alg.interfaces.MatchingAlgorithm
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import kotlin.random.Random

class JTournamentGraph(val teams: List<Team>):ITournamentGraph {
    val graph = SimpleWeightedGraph<Team, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)

    var mutablePairs = mutableListOf<Pair<Team,Team>>()
    var result: Graph<Team, DefaultWeightedEdge>? = null

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
    override fun runNodeMatching(randomSeed: Random){
        val matchingAlgorithm: MatchingAlgorithm<Team, DefaultWeightedEdge> =
            RandomizedGreedyMaximumCardinalityMatching(this.graph, seed = randomSeed)
        val output = matchingAlgorithm.matching
        this.result = output.graph

        for(edge in output.edges){
            val src = output.graph.getEdgeSource(edge) as Team
            val end = output.graph.getEdgeTarget(edge) as Team
//            println("${src.teamSignature} - ${end.teamSignature}")
            mutablePairs.add(Pair(src, end))
        }
    }

    override fun removeNode(team1: Team, team2: Team){
        this.graph.removeEdge(team1, team2)
    }

    override fun getMatching(): List<Pair<Team, Team>> {
        return mutablePairs
    }
}