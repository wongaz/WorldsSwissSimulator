package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.Match
import io.wongaz.model.MatchFactory
import io.wongaz.model.Team
import kotlin.random.Random

class StandardEloBasedNoRematches(val seed: Random, val matchFactory: MatchFactory): IMatchMakingRules {
    override fun performDraws(teams: List<Team>): List<Match> {
        TODO("Not yet implemented")
    }

}