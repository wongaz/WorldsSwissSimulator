package io.wongaz.tournamentplanner

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.matchsimulation.rules.PureEloSimulation
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlin.random.Random

@Component
abstract class WorldsSwissFormatSchedulerComponent(
    val seed: Random = Random.Default,
    @get:Provides protected val teams: List<Team>,
    @get:Provides val matchMakingRule: AbstractMatchMakingRule) {

    abstract val swissFormatScheduler: SwissFormatScheduler

    @Provides
    protected fun endCondition() : Int = 3

    @Provides
    protected fun bind() : IGameSimulation = PureEloSimulation(seed)
}