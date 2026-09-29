package io.wongaz.support

import io.wongaz.api.TeamStandingDto
import io.wongaz.api.TournamentDto
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun assertTournament(tournament: TournamentDto, signatures: Set<String>) {
    val records = signatures.associateWith { 0 to 0 }.toMutableMap()
    val opponents = mutableSetOf<Set<String>>()
    assertEquals(listOf(1, 2, 3, 4, 5), tournament.rounds.map { it.number })
    assertEquals(listOf(8, 8, 8, 6, 3), tournament.rounds.map { round -> round.groups.sumOf { it.matches.size } })
    tournament.rounds.forEach { round ->
        val active = records.filterValues { (wins, losses) -> wins < 3 && losses < 3 }.keys
        val played = mutableListOf<String>()
        round.groups.forEach { group ->
            assertEquals(round.number - 1, group.wins + group.losses)
            group.matches.forEach { match ->
                assertTrue(match.team1 != match.team2)
                assertEquals(group.wins to group.losses, records.getValue(match.team1))
                assertEquals(group.wins to group.losses, records.getValue(match.team2))
                assertTrue(opponents.add(setOf(match.team1, match.team2)), "Repeated opponent")
                val firstTo = if (group.wins == 2 || group.losses == 2) 2 else 1
                assertEquals(firstTo, match.firstTo)
                assertEquals(firstTo, maxOf(match.team1Wins, match.team2Wins))
                assertTrue(minOf(match.team1Wins, match.team2Wins) in 0 until firstTo)
                val winner = if (match.team1Wins > match.team2Wins) match.team1 else match.team2
                val loser = if (winner == match.team1) match.team2 else match.team1
                records[winner] = group.wins + 1 to group.losses
                records[loser] = group.wins to group.losses + 1
                played += listOf(match.team1, match.team2)
            }
        }
        assertEquals(active, played.toSet(), "Every active team appears in round ${round.number}")
        assertEquals(played.size, played.distinct().size)
    }
    val qualified = records.filterValues { it.first == 3 }.map { (id, record) ->
        TeamStandingDto(id, record.first, record.second)
    }.toSet()
    val eliminated = records.filterValues { it.second == 3 }.map { (id, record) ->
        TeamStandingDto(id, record.first, record.second)
    }.toSet()
    assertEquals(8, tournament.qualified.size)
    assertEquals(8, tournament.eliminated.size)
    assertEquals(qualified, tournament.qualified.toSet())
    assertEquals(eliminated, tournament.eliminated.toSet())
    assertEquals(listOf(2, 3, 3), (0..2).map { losses -> tournament.qualified.count { it.losses == losses } })
    assertEquals(listOf(2, 3, 3), (0..2).map { wins -> tournament.eliminated.count { it.wins == wins } })
}
