package io.wongaz.tournamentplanner

import me.tatarka.inject.annotations.Component

@Component
abstract class SwissFormatSchedulerComponent {
    abstract val swissFormatScheduler: SwissFormatScheduler

}