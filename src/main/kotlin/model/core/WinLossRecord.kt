package io.wongaz.model.core

class WinLossRecord(private val wins: Int, private val loss: Int){
    fun getWins(): Int{
        return this.wins
    }

    fun getLoss(): Int{
        return this.loss
    }

    override fun toString(): String {
        return "{$wins-$loss}"
    }
}