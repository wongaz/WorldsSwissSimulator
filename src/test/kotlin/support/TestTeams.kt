package io.wongaz.support

import io.wongaz.model.core.Category
import io.wongaz.model.core.Team

internal fun testTeam(number: Int, elo: Int = 1500): Team = Team(
    teamSignature = "T$number",
    teamName = "Team $number",
    region = "Region ${(number - 1) / 4}",
    seed = (number - 1) % 4 + 1,
    category = Category.entries[(number - 1) % Category.entries.size],
    elo = elo
)

internal fun testTeams(): List<Team> = (1..16).map { testTeam(it) }
