package io.wongaz.tournamentplanner

import io.wongaz.matchsimulation.IGameSimulation
import io.wongaz.matchsimulation.PureEloSimulation
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.model.core.Round
import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.IMatchMakingRules
import io.wongaz.tournamentplanner.matchmaking.NoEloNoRematchRule
import kotlin.random.Random

class SwissFormatScheduler (
    val endCondition: Int,
    private val teams: List<Team>,
    private val seed: Random){

    private val matchSimulation: IGameSimulation = PureEloSimulation(seed)

    private val matchFactory = MatchFactory(matchSimulation)
    private val matchMakingRules: IMatchMakingRules = NoEloNoRematchRule(seed, matchFactory)

    private val roundsList: List<Round> = mutableListOf()
    private val win = 0
    private val loss = 0

    private val qualified: MutableList<Team> = mutableListOf()
    private val eliminated: MutableList<Team> = mutableListOf()

    fun runTournament(){
        for (i in 0..<(2 * endCondition)){
            for (k in 0..0){
                val wins = k
                val losses = i-k

            }
        }

    }


}