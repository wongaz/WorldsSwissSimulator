package io.wongaz.tournamentplanner
import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.matchsimulation.rules.PureEloSimulation
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import io.wongaz.tournamentplanner.matchmaking.rules.NoEloNoRematchRule
import io.wongaz.tournamentplanner.scheduler.SwissFormatScheduler
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlin.random.Random

@Component
abstract class WorldsSwissFormatSchedulerComponent(
    val seed: Random = Random.Default,
    @get:Provides protected val teams: List<Team>,
    @get:Provides protected val gameSimulation : IGameSimulation = PureEloSimulation(seed),
    @get:Provides protected val matchMakingRule: AbstractMatchMakingRule = NoEloNoRematchRule(seed)) {

    abstract val swissFormatScheduler: SwissFormatScheduler

    @Provides
    protected fun endCondition() : Int = 3
}