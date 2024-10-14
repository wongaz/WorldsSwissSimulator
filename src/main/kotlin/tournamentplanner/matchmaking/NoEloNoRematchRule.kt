package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.core.Match
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.TournamentGraph
import kotlin.random.Random

/**
 * Purely Random protecting against rematches from past rounds
 */
class NoEloNoRematchRule(val seed: Random, val matchFactory: MatchFactory): IMatchMakingRules {
    override fun performDraws(teams: List<Team>, fto: Int): List<Match> {
        val tournamentGraph = TournamentGraph(teams)
        super.removeRematches(tournamentGraph, teams)

        tournamentGraph.runNodeMatching(this.seed)
        val matchesCount = teams.size / 2
        var output = this.getMatches(tournamentGraph, this.matchFactory, matchesCount, fto)
        while(output.isNotEmpty()){
            tournamentGraph.reset()
        }
        return output
    }
}