package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.core.Match
import io.wongaz.model.core.Team

interface IMatchMakingRules {
    fun performDraws(teams: List<Team>): List<Match>
}