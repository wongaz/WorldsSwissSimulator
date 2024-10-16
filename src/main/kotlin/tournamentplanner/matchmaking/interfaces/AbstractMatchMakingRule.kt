package io.wongaz.tournamentplanner.matchmaking.interfaces

import io.wongaz.model.core.Match
import io.wongaz.model.core.Team
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.tournamentplanner.matchmaking.graph.JTournamentGraph
import io.wongaz.tournamentplanner.matchmaking.graph.interfaces.ITournamentGraph
import me.tatarka.inject.annotations.Inject
import kotlin.random.Random

@Inject
abstract class AbstractMatchMakingRule(val seed: Random = Random.Default){

    fun generateMatchPairs(teams: List<Team>, matchFactory: MatchFactory, fto: Int = 1) : List<Match>{
        if (teams.isEmpty()) return emptyList()
        val tournamentGraph: ITournamentGraph = JTournamentGraph(teams)

        removeMatches(tournamentGraph, teams)
        updateWeights(tournamentGraph, teams)

        tournamentGraph.runNodeMatching(this.seed)

        val matchesCount = teams.size / 2
        var output = this.getMatches(tournamentGraph, matchFactory, matchesCount, fto)

        while(output.isEmpty()){
            unblock(tournamentGraph, teams)
            tournamentGraph.runNodeMatching(this.seed)
            output = this.getMatches(tournamentGraph, matchFactory, matchesCount, fto)
        }
        return output
    }

    abstract fun removeMatches(tournamentGraph: ITournamentGraph, teams: List<Team>)
    abstract fun updateWeights(tournamentGraph: ITournamentGraph, teams: List<Team>)
    abstract fun unblock(tournamentGraph: ITournamentGraph, teams: List<Team>)

    fun removeRematches(tGraph: ITournamentGraph, teams: List<Team>){
        for(team in teams){
            for(pastMatchup in team.getPreviousPlayedTeams()){
                tGraph.removeNode(team, pastMatchup)
            }
        }
    }

    fun getMatches(tGraph: ITournamentGraph, matchFactory: MatchFactory, k: Int, fto: Int): List<Match> {
        val matches = tGraph.getMatching()
        if (matches.size == k){
            return matches
                .map { x -> matchFactory.buildMatch(x.first, x.second, fto) }
                .toList()
        }
        return emptyList()
    }
}