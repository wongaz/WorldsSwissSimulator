package io.wongaz.simulationmanager

import io.wongaz.model.core.WinLossRecord
import io.wongaz.simulationmanager.interfaces.AbstractSimManager
import io.wongaz.simulationmanager.sims.MultiThreadedSimManager
import io.wongaz.simulationmanager.sims.SingleThreadedSimManager
import io.wongaz.support.testTeams
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Timeout(20)
class SimulationManagerTest {
    @Test
    fun `single threaded manager aggregates every iteration without mutating input teams`() {
        verify(SingleThreadedSimManager(testTeams(), iterations = 3), iterations = 3)
    }

    @Test
    fun `parallel workers cover uneven partitions and excess workers exactly once`() {
        for ((iterations, workers) in listOf(5 to 2, 3 to 8, 1 to 1, 3 to 0)) {
            verify(MultiThreadedSimManager(testTeams(), iterations, workers), iterations)
        }
    }

    private fun verify(manager: AbstractSimManager, iterations: Int) {
        assertTrue(manager.getResults().all { it.qualification == 0.0 })

        manager.doWork()

        val results = manager.getResults()
        assertEquals(16, results.size)
        assertEquals(8.0 * iterations, results.sumOf { it.qualification })
        results.forEach {
            assertTrue(it.qualification in 0.0..iterations.toDouble())
            assertEquals(it.qualification.toInt().toDouble(), it.qualification)
            assertEquals(it.qualification * 100.0 / iterations, it.getPercentage(), 0.000001)
        }
        manager.teams.forEach {
            assertTrue(it.equalsWinLoss(WinLossRecord(0, 0)))
            assertTrue(it.getPreviousPlayedTeams().isEmpty())
        }
    }
}
