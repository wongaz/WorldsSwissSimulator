package io.wongaz.tournamentplanner.matchmaking.graph

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.component.Edge
import io.wongaz.tournamentplanner.matchmaking.component.INode
import io.wongaz.tournamentplanner.matchmaking.graph.node.TeamNode
import kotlin.random.Random

class TournamentGraph(teams: List<Team>) {
    private val teamToNodeMap = mutableMapOf<Team, INode>()
    private val nodeMap = mutableMapOf<INode, MutableSet<Edge>>()

    private var hasRun = false

    init {
        makeCompleteGraph(teams)
    }

    private fun makeCompleteGraph(teams: List<Team>){
        for (team in teams) {
            val teamNode = TeamNode(team)
            this.nodeMap[teamNode] = mutableSetOf()
            this.teamToNodeMap[team] = teamNode
        }

        for((index, team1) in teams.withIndex()){
            val teamNode1 = this.teamToNodeMap[team1]!!
            for(k in index+1..<teams.size){
                val team2 = teams[k]
                val teamNode2 = this.teamToNodeMap[team2]!!
                val edge = Edge(teamNode1, teamNode2)

                val teamSet1 = this.nodeMap[teamNode1]!!
                teamSet1.add(edge)
                val teamSet2 = this.nodeMap[teamNode2]!!
                teamSet2.add(edge)
            }
        }
    }

    fun removeNode(edge: Edge){
        // Mark as Ignorable
    }

    fun runNodeMatching(randomSeed: Random){
        this.hasRun = true
    }

    fun reset() {
        for (edge in nodeMap.flatMap { (K,V)-> V.toList() })
            edge.resetState()
        this.hasRun = false
    }

    fun getMatching(): List<Edge>? {
        if (!this.hasRun) {
            return null
        }
        return this.nodeMap.flatMap { (K,V)-> V.toList() }.filter { it.isIncluded() }
    }
}