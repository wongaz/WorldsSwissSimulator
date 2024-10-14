package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.core.Match
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.TournamentGraph
import kotlin.random.Random

class PurelyRandom(seed: Random, matchFactory: MatchFactory): AbstractMatchMakingRule(seed, matchFactory) {
    override fun removeMatches(tournamentGraph: TournamentGraph, teams: List<Team>) {
        super.removeRematches(tournamentGraph, teams)
    }

    override fun updateWeights(tournamentGraph: TournamentGraph, teams: List<Team>) {
    }

    override fun unblock(tournamentGraph: TournamentGraph, teams: List<Team>) {

    }
}