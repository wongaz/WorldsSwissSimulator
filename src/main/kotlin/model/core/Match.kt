package io.wongaz.model.core

import io.wongaz.matchsimulation.interfaces.IGameSimulation

class Match(val team1: Team,
            val team2: Team,
            val firstTo: Int,
            private val simulation: IGameSimulation
) {

    private var teamWin1 = 0
    private var teamWin2 = 0

    init {
        simulateMatch()
    }

    private fun simulateMatch(){
        do {
            val winner = simulation.runSingleGameSimulation(team1, team2)
            if(winner.teamSignature == team1.teamSignature) {
                teamWin1++
            }else{
                teamWin2++
            }

        } while(teamWin1 != firstTo && teamWin2 != firstTo)
    }

    fun getOtherTeam(team: Team): Team {
        if (this.team1.teamSignature == team.teamSignature) {
            return team2
        }
        return team1
    }

    fun getWinner(): Team?{
        if (teamWin1 == teamWin2) {
            return null
        }
        if(teamWin1 > teamWin2) {
            return team1
        }
        return team1
    }

    fun getLoser(): Team?{
        val winner = this.getWinner() ?: return null
        if (team1 == winner){
            return team2
        }
        return team1
    }

    override fun toString(): String {
        return "${team1.teamSignature} $teamWin1 - $teamWin2 ${team2.teamSignature}"
    }
}