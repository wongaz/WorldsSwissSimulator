package io.wongaz.model.core

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val teamSignature: String,
    val teamName: String,
    val region: String,
    val seed: Int,
    val category: Category,
    val elo: Int){

    private var winningMatches: MutableList<Match> = mutableListOf()
    private var lossMatches: MutableList<Match> = mutableListOf()

    fun addMatch(match: Match){
        if (this.teamSignature == match.getWinner()?.teamSignature){
            this.winningMatches.add(match)
        }else {
            this.lossMatches.add(match)
        }
    }

    fun hasPlayed(team: Team): Boolean{
        for (match in this.winningMatches){
            val otherTeam = match.getOtherTeam(this)
            if (team == otherTeam){
                return true
            }
        }
        for (match in this.lossMatches){
            val otherTeam = match.getOtherTeam(this)
            if (team == otherTeam){
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return this.teamSignature
    }

    fun getPreviousPlayedTeams(): List<Team>{
        return this.winningMatches.map { x -> x.getOtherTeam(this) } +
                this.lossMatches.map { y -> y.getOtherTeam(this)}
    }

    fun equalsWinLoss(winLossRecord: WinLossRecord): Boolean{
        return winningMatches.size == winLossRecord.getWins() && winLossRecord.getLoss() == lossMatches.size
    }
}
