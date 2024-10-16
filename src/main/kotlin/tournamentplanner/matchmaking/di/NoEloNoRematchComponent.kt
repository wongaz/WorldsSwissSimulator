package io.wongaz.tournamentplanner.matchmaking.di

import io.wongaz.tournamentplanner.matchmaking.rules.NoEloNoRematchRule
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlin.random.Random

@Component
abstract class NoEloNoRematchComponent(
    @get:Provides val seed: Random = Random.Default) {

    abstract val matchMakingRule: NoEloNoRematchRule
}