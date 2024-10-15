package io.wongaz.tournamentplanner.matchmaking.rules

import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import io.wongaz.tournamentplanner.matchmaking.graph.interfaces.ITournamentGraph
import kotlin.random.Random

class StandardEloBasedRule(seed: Random, matchFactory: MatchFactory): AbstractMatchMakingRule(seed, matchFactory) {
    override fun removeMatches(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        super.removeRematches(tournamentGraph, teams)
    }

    override fun updateWeights(tournamentGraph: ITournamentGraph, teams: List<Team>) {
    }

    override fun unblock(tournamentGraph: ITournamentGraph, teams: List<Team>) {

    }
}