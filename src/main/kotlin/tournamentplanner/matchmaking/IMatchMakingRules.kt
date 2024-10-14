package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.core.Match
import io.wongaz.model.core.Team
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.tournamentplanner.matchmaking.graph.Edge
import io.wongaz.tournamentplanner.matchmaking.graph.TournamentGraph

interface IMatchMakingRules {
    fun performDraws(teams: List<Team>, fto: Int = 1): List<Match>

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