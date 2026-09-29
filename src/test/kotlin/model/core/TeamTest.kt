package io.wongaz.model.core

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.support.testTeam
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class TeamTest {
    @Test
    fun `match history records wins losses and both opponents`() {
        val team = testTeam(1)
        val beaten = testTeam(2)
        val winner = testTeam(3)
        team.addMatch(matchWonBySecond(beaten, team))
        team.addMatch(matchWonBySecond(team, winner))

        assertTrue(team.equalsWinLoss(WinLossRecord(1, 1)))
        assertFalse(team.equalsWinLoss(WinLossRecord(2, 0)))
        assertTrue(team.hasPlayed(beaten))
        assertTrue(team.hasPlayed(winner.copy()))
        assertFalse(team.hasPlayed(testTeam(4)))
        assertEquals(listOf(beaten, winner), team.getPreviousPlayedTeams())
    }

    @Test
    fun `copy preserves metadata but resets history without modifying original`() {
        val team = testTeam(1)
        val opponent = testTeam(2)
        team.addMatch(matchWonBySecond(opponent, team))

        val copy = team.copy()

        assertEquals(team, copy)
        assertNotSame(team, copy)
        assertTrue(copy.equalsWinLoss(WinLossRecord(0, 0)))
        assertTrue(copy.getPreviousPlayedTeams().isEmpty())
        assertFalse(copy.hasPlayed(opponent))
        copy.addMatch(matchWonBySecond(copy, testTeam(3)))
        assertTrue(copy.equalsWinLoss(WinLossRecord(0, 1)))
        assertTrue(team.equalsWinLoss(WinLossRecord(1, 0)))
        assertEquals(listOf(opponent), team.getPreviousPlayedTeams())
    }

    private fun matchWonBySecond(first: Team, second: Team): Match =
        Match(first, second, 1, object : IGameSimulation {
            override fun runSingleGameSimulation(team1: Team, team2: Team): Team = team2
        })
}
