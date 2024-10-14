package io.wongaz.tournamentplanner.matchmaking.graph

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.node.TeamNode

class Edge(
    val node1: INode,
    val node2: INode,
    val heuristicOverride: Int = 1) {

    constructor(team1: Team, team2: Team) :
            this(TeamNode(team1), TeamNode(team2))

    private var included = false
    var ignore = false

    fun getOtherNode(incomingNode: INode): INode? {
        if (node1 == incomingNode){
            return node2
        }
        if (node2 == incomingNode){
            return node1
        }
        return null
    }

    fun markAsIgnorable(){
        this.ignore = true
    }

    fun isIncluded(): Boolean {
        return this.included
    }

    fun resetState(){
        this.included = false
    }

    fun getTeams(): Pair<Team, Team> {
        val teamNode1: TeamNode = node1 as TeamNode
        val teamNode2: TeamNode = node2 as TeamNode
        return Pair(teamNode1.team, teamNode2.team)
    }

    override fun hashCode(): Int {
        return node1.hashCode() + node2.hashCode()
    }
}