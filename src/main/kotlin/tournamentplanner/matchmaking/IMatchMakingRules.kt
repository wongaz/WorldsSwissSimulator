package io.wongaz.tournamentplanner.matchmaking

import io.wongaz.model.Match
import io.wongaz.model.Team

interface IMatchMakingRules {
    fun performDraws(teams: List<Team>): List<Match>
}