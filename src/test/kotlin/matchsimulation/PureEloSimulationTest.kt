package io.wongaz.matchsimulation

import io.wongaz.matchsimulation.rules.PureEloSimulation
import io.wongaz.support.testTeam
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertSame

class PureEloSimulationTest {
    @Test
    fun `equal elo uses the supplied random draw`() {
        val first = testTeam(1)
        val second = testTeam(2)

        assertSame(first, PureEloSimulation(FixedRandom(0.25)).runSingleGameSimulation(first, second))
        assertSame(second, PureEloSimulation(FixedRandom(0.75)).runSingleGameSimulation(first, second))
    }

    @Test
    fun `overwhelming elo advantage wins in either position`() {
        val favorite = testTeam(1, elo = 100_000)
        val underdog = testTeam(2, elo = 0)
        val simulation = PureEloSimulation(FixedRandom(0.5))

        assertSame(favorite, simulation.runSingleGameSimulation(favorite, underdog))
        assertSame(favorite, simulation.runSingleGameSimulation(underdog, favorite))
    }

    private class FixedRandom(private val draw: Double) : Random() {
        override fun nextBits(bitCount: Int): Int = error("This test supplies nextDouble directly.")
        override fun nextDouble(): Double = draw
    }
}
