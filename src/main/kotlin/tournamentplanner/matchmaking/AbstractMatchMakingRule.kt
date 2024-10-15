package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.core.Match
import io.wongaz.model.core.Team
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.tournamentplanner.matchmaking.component.Edge
import io.wongaz.tournamentplanner.matchmaking.graph.TournamentGraph
import kotlin.random.Random

abstract class AbstractMatchMakingRule(val seed: Random, val matchFactory: MatchFactory){

    fun generateMatchPairs(teams: List<Team>, fto: Int = 1) : List<Match>{
        val tournamentGraph = TournamentGraph(teams)

        removeMatches(tournamentGraph, teams)
        updateWeights(tournamentGraph, teams)

        tournamentGraph.runNodeMatching(this.seed)

        val matchesCount = teams.size / 2
        var output = this.getMatches(tournamentGraph, this.matchFactory, matchesCount, fto)

        while(output.isNotEmpty()){
            tournamentGraph.reset()
            unblock(tournamentGraph, teams)

            tournamentGraph.runNodeMatching(this.seed)
            output = this.getMatches(tournamentGraph, this.matchFactory, matchesCount, fto)
        }
        return output
    }

    abstract fun removeMatches(tournamentGraph: TournamentGraph, teams: List<Team>)
    abstract fun updateWeights(tournamentGraph: TournamentGraph, teams: List<Team>)
    abstract fun unblock(tournamentGraph: TournamentGraph, teams: List<Team>)

    fun removeRematches(tGraph: TournamentGraph, teams: List<Team>){
        for(team in teams){
            for(pastMatchup in team.getPreviousPlayedTeams()){
                val edg = Edge(team, pastMatchup)
                tGraph.removeNode(edg)
            }
        }
    }

    fun getMatches(tGraph: TournamentGraph, matchFactory: MatchFactory, k: Int, fto: Int): List<Match> {
        val matches = tGraph.getMatching()!!.filter { it.isIncluded() }
        if (matches.size == k){
            return matches
                .map { x -> x.getTeams() }
                .map { x -> matchFactory.buildMatch(x.first, x.second, fto) }
                .toList()
        }
        return emptyList()
    }
}