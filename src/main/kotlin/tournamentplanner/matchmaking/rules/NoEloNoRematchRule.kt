package io.wongaz.tournamentplanner.matchmaking.rules

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.interfaces.ITournamentGraph
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import me.tatarka.inject.annotations.Inject
import kotlin.random.Random

/**
 * Purely Random protecting against rematches from past rounds
 * 16 Monkeys in a barrel
 */
@Inject
class NoEloNoRematchRule(seed: Random):
    AbstractMatchMakingRule(seed) {

    override fun removeMatches(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        super.removeRematches(tournamentGraph, teams)
    }

    override fun updateWeights(tournamentGraph: ITournamentGraph, teams: List<Team>) {
    }

    override fun unblock(tournamentGraph: ITournamentGraph, teams: List<Team>) {
    }

}