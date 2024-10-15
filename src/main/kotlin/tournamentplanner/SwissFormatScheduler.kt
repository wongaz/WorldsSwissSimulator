package io.wongaz.tournamentplanner

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.matchsimulation.rules.PureEloSimulation
import io.wongaz.model.core.*
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.tournamentplanner.matchmaking.AbstractMatchMakingRule
import io.wongaz.tournamentplanner.matchmaking.NoEloNoRematchRule
import kotlin.random.Random

class SwissFormatScheduler (
    private val endCondition: Int,
    private val teams: List<Team>,
    private val seed: Random = Random.Default) {

    private val matchSimulation: IGameSimulation = PureEloSimulation(this.seed)

    private val matchFactory = MatchFactory(matchSimulation)
    private val matchMakingRules: AbstractMatchMakingRule = NoEloNoRematchRule(this.seed, matchFactory)

    private val roundsList = mutableListOf<Round>()

    private val qualified: MutableList<Team> = mutableListOf()
    private val eliminated: MutableList<Team> = mutableListOf()

    fun runTournament(){
        for (i in 0 ..<(2 * endCondition)-1){
            val currentRound = Round(i+1)
            this.roundsList.add(currentRound)

            println(currentRound)

            for (k in 0..i){
                val wins = k
                val losses = i-k

                if(wins >= endCondition || losses >= endCondition) {
                    continue
                }

                val winLossRecord = WinLossRecord(wins, losses)
                println(winLossRecord)
                val filteredTeams = this.teams.filter { it.equalsWinLoss(winLossRecord) }
                var matches = emptyList<Match>()
                if (wins == 2 || losses == 2){
                    matches = this.matchMakingRules.generateMatchPairs(filteredTeams, 2)
                }else {
                    matches = this.matchMakingRules.generateMatchPairs(filteredTeams)
                }
                println(matches)

                currentRound.addPool(winLossRecord, Pool(matches))
                for (match in matches){
                    val winner = match.getWinner()!!
                    val loser = match.getLoser()!!

                    winner.addMatch(match)
                    loser.addMatch(match)

                    if(wins == 2){
                        qualified.add(winner)
                        println("Qualify: ${winner.teamSignature}")
                    }
                    if(losses == 2) {
                        eliminated.add(loser)
                        println("Eliminated: ${loser.teamSignature}")
                    }
                }
            }
        }
    }

    fun getEliminatedTeams(): List<Team>{
        return this.eliminated.toList()
    }

    fun getQualifiedTeams(): List<Team>{
        return this.qualified.toList()
    }


}