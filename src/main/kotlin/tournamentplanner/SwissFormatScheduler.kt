package io.wongaz.tournamentplanner

import io.wongaz.matchsimulation.IGameSimulation
import io.wongaz.matchsimulation.PureEloSimulation
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.model.core.Round
import io.wongaz.model.core.Team
import io.wongaz.model.core.WinLossRecord
import io.wongaz.tournamentplanner.matchmaking.AbstractMatchMakingRule
import io.wongaz.tournamentplanner.matchmaking.NoEloNoRematchRule
import io.wongaz.tournamentplanner.matchmaking.graph.JTournamentGraph
import kotlin.random.Random

class SwissFormatScheduler (
    private val endCondition: Int,
    private val teams: List<Team>,
    private val seed: Random){

    private val matchSimulation: IGameSimulation = PureEloSimulation(seed)

    private val matchFactory = MatchFactory(matchSimulation)
    private val matchMakingRules: AbstractMatchMakingRule = NoEloNoRematchRule(seed, matchFactory)

    private val roundsList: List<Round> = mutableListOf()
    private val win = 0
    private val loss = 0

    private val qualified: MutableList<Team> = mutableListOf()
    private val eliminated: MutableList<Team> = mutableListOf()

    fun runTournament(){
        for (i in 0 .. 2 * endCondition){
            for (k in 0..i){
                val wins = k
                val losses = i-k
                val winLossRecord = WinLossRecord(wins, losses)
                println(winLossRecord)
                val filteredTeams = teams.filter { it.equalsWinLoss(winLossRecord) }
                val matches = this.matchMakingRules.generateMatchPairs(filteredTeams)
                println(matches)
            }
        }
    }

    fun getEliminatedTeams(): List<Team>{
        return eliminated.toList()
    }

    fun getQualified(): List<Team>{
        return qualified.toList()
    }


}