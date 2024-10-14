package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.core.Match
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.model.core.Team
import kotlin.random.Random

class StandardEloBasedNoRematches(val seed: Random, val matchFactory: MatchFactory): IMatchMakingRules {
    override fun performDraws(teams: List<Team>, fto: Int): List<Match> {
        TODO("Not yet implemented")
    }

}