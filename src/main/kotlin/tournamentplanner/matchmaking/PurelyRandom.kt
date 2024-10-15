package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.core.Match
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.TournamentGraph
import io.wongaz.tournamentplanner.matchmaking.graph.interfaces.ITournamentGraph
import kotlin.random.Random

class PurelyRandom(seed: Random, matchFactory: MatchFactory): AbstractMatchMakingRule(seed, matchFactory) {
    override fun removeMatches(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        TODO("Not yet implemented")
    }

    override fun updateWeights(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        TODO("Not yet implemented")
    }

    override fun unblock(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        TODO("Not yet implemented")
    }
}