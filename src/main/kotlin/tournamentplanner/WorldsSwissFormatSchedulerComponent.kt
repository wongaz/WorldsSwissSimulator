package io.wongaz.tournamentplanner

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class WorldsSwissFormatSchedulerComponent(
    @get:Provides protected val teams: List<Team>,
    @Component val gameSimulations: IGameSimulation,
    @Component val matchMakingRule: AbstractMatchMakingRule) {

    abstract val swissFormatScheduler: SwissFormatScheduler

    @Provides
    protected fun endCondition() : Int = 3
}