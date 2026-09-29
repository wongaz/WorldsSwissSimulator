package io.wongaz.tournamentplanner

import io.wongaz.matchsimulation.rules.EquallyFavoredSim
import io.wongaz.model.core.WinLossRecord
import io.wongaz.support.testTeams
import io.wongaz.tournamentplanner.matchmaking.rules.NoEloNoRematchRule
import io.wongaz.tournamentplanner.scheduler.SwissFormatScheduler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Timeout(15)
class SwissFormatSchedulerTest {
    @Test
    fun `sixteen teams finish with eight qualified eight eliminated and no rematches`() {
        for (seed in listOf(1, 42, 2024)) {
            val teams = testTeams()
            val scheduler = SwissFormatScheduler(
                teams = teams,
                gameSimulation = EquallyFavoredSim(Random(seed)),
                matchMakingRule = NoEloNoRematchRule(Random(seed))
            )

            scheduler.runTournament()

            val qualified = scheduler.getQualifiedTeams()
            val eliminated = scheduler.getEliminatedTeams()
            assertEquals(8, qualified.size, "seed=$seed")
            assertEquals(8, eliminated.size, "seed=$seed")
            assertEquals(teams.toSet(), (qualified + eliminated).toSet())
            assertTrue(qualified.toSet().intersect(eliminated.toSet()).isEmpty())
            qualified.forEach { team ->
                assertTrue((0..2).any { team.equalsWinLoss(WinLossRecord(3, it)) })
            }
            eliminated.forEach { team ->
                assertTrue((0..2).any { team.equalsWinLoss(WinLossRecord(it, 3)) })
            }
            teams.forEach { team ->
                val opponents = team.getPreviousPlayedTeams()
                assertTrue(opponents.size in 3..5)
                assertEquals(opponents.size, opponents.distinct().size, "$team repeated an opponent, seed=$seed")
                assertTrue(opponents.none { it.teamSignature == team.teamSignature })
                assertTrue(opponents.all { it.hasPlayed(team) })
            }
        }
    }
}
