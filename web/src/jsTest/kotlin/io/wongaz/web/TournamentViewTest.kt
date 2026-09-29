package io.wongaz.web

import io.wongaz.api.PairingDto
import io.wongaz.api.PairingGroupDto
import io.wongaz.api.TeamStandingDto
import io.wongaz.api.TournamentDto
import io.wongaz.api.TournamentRoundDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TournamentViewTest {
    @Test
    fun placesEachTerminalRecordOnlyInTheNextColumn() {
        val teams = listOf(
            TeamStandingDto("A", 3, 0), TeamStandingDto("B", 0, 3),
            TeamStandingDto("C", 3, 1), TeamStandingDto("D", 1, 3),
            TeamStandingDto("E", 3, 2), TeamStandingDto("F", 2, 3)
        )
        for (column in 1..3) assertTrue(terminalTeamsForColumn(teams, column).isEmpty())
        assertEquals(listOf("A", "B"), terminalTeamsForColumn(teams, 4).map { it.teamSignature })
        assertEquals(listOf("C", "D"), terminalTeamsForColumn(teams, 5).map { it.teamSignature })
        assertEquals(listOf("E", "F"), terminalTeamsForColumn(teams, 6).map { it.teamSignature })
        assertEquals(teams, (1..6).flatMap { terminalTeamsForColumn(teams, it) })
    }

    @Test
    fun pathIncludesAllRoundsInOrderWithTeamRelativeScoresAndRecords() {
        val tournament = TournamentDto(
            1,
            listOf(
                round(5, 2, 2, PairingDto("A", "F", 2, 1, 2)),
                round(3, 1, 1, PairingDto("A", "D", 0, 1, 1)),
                round(1, 0, 0, PairingDto("A", "B", 1, 0, 1)),
                round(4, 1, 2, PairingDto("E", "A", 1, 2, 2)),
                round(2, 1, 0, PairingDto("C", "A", 1, 0, 1))
            ),
            listOf(TeamStandingDto("A", 3, 2)),
            listOf(TeamStandingDto("F", 2, 3))
        )
        val path = tournamentPath(tournament, "A")
        assertEquals(
            listOf(
                TeamPathStep(1, "B", 1, 0, 1, 0),
                TeamPathStep(2, "C", 0, 1, 1, 1),
                TeamPathStep(3, "D", 0, 1, 1, 2),
                TeamPathStep(4, "E", 2, 1, 2, 2),
                TeamPathStep(5, "F", 2, 1, 3, 2)
            ),
            path
        )
        assertEquals(listOf(true, false, false, true, true), path.map { it.won })
        assertTrue(tournamentPath(tournament, "missing").isEmpty())
        assertEquals(TeamPathStep(5, "A", 1, 2, 2, 3), tournamentPath(tournament, "F").single())
        assertFalse(tournamentPath(tournament, "F").single().won)
    }

    @Test
    fun pathStopsWhenTeamQualifiesOrIsEliminated() {
        val tournament = TournamentDto(
            1,
            listOf(
                round(1, 0, 0, PairingDto("A", "B", 1, 0, 1)),
                round(2, 1, 0, PairingDto("A", "C", 1, 0, 1)),
                round(3, 2, 0, PairingDto("D", "A", 0, 2, 2)),
                round(4, 2, 1, PairingDto("D", "E", 2, 1, 2)),
                round(5, 2, 2, PairingDto("E", "F", 2, 0, 2))
            ),
            listOf(TeamStandingDto("A", 3, 0)),
            emptyList()
        )
        assertEquals(listOf(1, 2, 3), tournamentPath(tournament, "A").map { it.round })
        assertEquals(3, tournamentPath(tournament, "A").last().wins)
        val eliminated = tournament.copy(
            rounds = (1..3).map { number ->
                round(number, 0, number - 1, PairingDto("A", "B", 0, if (number == 3) 2 else 1, if (number == 3) 2 else 1))
            },
            qualified = emptyList(),
            eliminated = listOf(TeamStandingDto("A", 0, 3))
        )
        assertEquals(3, tournamentPath(eliminated, "A").last().losses)
        assertTrue(tournamentPath(eliminated, "A").none { it.won })
    }

    private fun round(number: Int, wins: Int, losses: Int, match: PairingDto) =
        TournamentRoundDto(number, listOf(PairingGroupDto(wins, losses, listOf(match))))
}
