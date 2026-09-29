package io.wongaz.model.core

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.support.testTeam
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MatchTest {
    private val team1 = testTeam(1)
    private val team2 = testTeam(2)

    @Test
    fun `best of one returns team two as winner and team one as loser`() {
        val simulation = ScriptedSimulation(listOf(team2))

        val match = Match(team1, team2, 1, simulation)

        assertSame(team2, match.getWinner())
        assertSame(team1, match.getLoser())
        assertEquals(1, simulation.calls)
        assertEquals("T1 0 - 1 T2", match.toString())
    }

    @Test
    fun `best of three stops after a sweep`() {
        val simulation = ScriptedSimulation(listOf(team2, team2))

        val match = Match(team1, team2, 2, simulation)

        assertSame(team2, match.getWinner())
        assertSame(team1, match.getLoser())
        assertEquals(2, simulation.calls)
    }

    @Test
    fun `best of three plays a decider when teams split first two games`() {
        val simulation = ScriptedSimulation(listOf(team2, team1, team1))

        val match = Match(team1, team2, 2, simulation)

        assertSame(team1, match.getWinner())
        assertSame(team2, match.getLoser())
        assertEquals(3, simulation.calls)
        assertEquals("T1 2 - 1 T2", match.toString())
    }

    @Test
    fun `opponent lookup identifies equivalent team snapshots`() {
        val match = Match(team1, team2, 1, ScriptedSimulation(listOf(team1)))

        assertSame(team2, match.getOtherTeam(team1.copy()))
        assertSame(team1, match.getOtherTeam(team2.copy()))
    }

    @Test
    fun `nonpositive target is rejected before simulating`() {
        val simulation = ScriptedSimulation(emptyList())

        for (target in listOf(0, -1)) {
            assertFailsWith<IllegalArgumentException> { Match(team1, team2, target, simulation) }
        }
        assertEquals(0, simulation.calls)
    }

    private class ScriptedSimulation(private val winners: List<Team>) : IGameSimulation {
        var calls = 0
            private set

        override fun runSingleGameSimulation(team1: Team, team2: Team): Team = winners[calls++]
    }
}
