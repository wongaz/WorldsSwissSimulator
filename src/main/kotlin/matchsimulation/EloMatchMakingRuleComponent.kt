package io.wongaz.matchsimulation

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.matchsimulation.rules.PureEloSimulation
import io.wongaz.tournamentplanner.SwissFormatScheduler
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlin.random.Random

@Component
abstract class EloMatchMakingRuleComponent(@get:Provides val random: Random = Random.Default){
    abstract val pureEloSimulation: PureEloSimulation
}