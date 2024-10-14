package io.wongaz.tournamentplanner.matchmaking.graph

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.node.TeamNode

class TournamentGraph(teams: List<Team>) {
    private val teamToNodeMap = mutableMapOf<Team,INode>()
    private val nodeMap = mutableMapOf<INode, MutableSet<Edge>>()

    init {

    }

    private fun makeCompleteGraph(teams: List<Team>){
        for (team in teams) {
            val teamNode = TeamNode(team)
            this.nodeMap[teamNode] = mutableSetOf()
            this.teamToNodeMap[team] = teamNode
        }

        for((index, team1) in teams.withIndex()){
            val teamNode1 = teamToNodeMap.get(team1)!!
            for(k in index+1..<teams.size){
                val team2 = teams[k]
                val teamNode2 = teamToNodeMap.get(team2)!!
                val edge = Edge(teamNode1, teamNode2)
            }
        }

    }


}