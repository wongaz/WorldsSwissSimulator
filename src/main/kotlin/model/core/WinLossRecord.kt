package io.wongaz.model.core

class WinLossRecord(private val wins: Short, private val loss: Short){
    fun getWins(): Short{
        return this.wins
    }

    fun getLoss(): Short{
        return this.loss
    }

    override fun toString(): String {
        return "{$wins-$loss}"
    }
}