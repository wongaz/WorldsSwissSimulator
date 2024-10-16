package io.wongaz.tournamentplanner.scheduler

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.model.core.*
import io.wongaz.model.core.factory.MatchFactory
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import me.tatarka.inject.annotations.Inject

@Inject
class SwissFormatScheduler (
    private val endCondition: Int = 3,
    private val teams: List<Team>,
    private val gameSimulation: IGameSimulation,
    private val matchMakingRule: AbstractMatchMakingRule) {

    private val matchFactory = MatchFactory(this.gameSimulation)

    private val roundsList = mutableListOf<Round>()

    private val qualified: MutableList<Team> = mutableListOf()
    private val eliminated: MutableList<Team> = mutableListOf()

    fun runTournament(){
        for (i in 0 ..<(2 * endCondition)-1){
            val currentRound = Round(i+1)
            this.roundsList.add(currentRound)

            for (k in 0..i){
                val wins = k
                val losses = i-k

                if(wins >= endCondition || losses >= endCondition) {
                    continue
                }

                val winLossRecord = WinLossRecord(wins, losses)
                val filteredTeams = this.teams.filter { it.equalsWinLoss(winLossRecord) }
                var matches: List<Match>
                if (wins == endCondition - 1  || losses == endCondition - 1 ){
                    matches = this.matchMakingRule.generateMatchPairs(filteredTeams, this.matchFactory,2)
                }else {
                    matches = this.matchMakingRule.generateMatchPairs(filteredTeams,this.matchFactory)
                }

                currentRound.addPool(winLossRecord, Pool(matches))
                for (match in matches){
                    val winner = match.getWinner()!!
                    val loser = match.getLoser()!!

                    winner.addMatch(match)
                    loser.addMatch(match)

                    if(wins == 2){
                        qualified.add(winner)
//                        println("Qualify: ${winner.teamSignature}")
                    }
                    if(losses == 2) {
                        eliminated.add(loser)
//                        println("Eliminated: ${loser.teamSignature}")
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