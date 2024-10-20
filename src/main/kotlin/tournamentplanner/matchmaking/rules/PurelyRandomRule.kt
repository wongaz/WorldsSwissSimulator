package io.wongaz.tournamentplanner.matchmaking.rules

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.interfaces.ITournamentGraph
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import kotlin.random.Random

class PurelyRandomRule(seed: Random): AbstractMatchMakingRule(seed) {
    override fun removeMatches(tournamentGraph: ITournamentGraph, teams: List<Team>) {

    }

    override fun updateWeights(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        TODO("Not yet implemented")
    }

    override fun unblock(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        TODO("Not yet implemented")
    }
}