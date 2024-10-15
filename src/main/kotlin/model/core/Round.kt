package io.wongaz.model.core

data class Round(private val number: Int) {
    private val matchesMap = mutableMapOf<WinLossRecord, Pool>()

    fun addPool(winLossRecord: WinLossRecord, pool: Pool) {
        matchesMap.put(winLossRecord, pool)
    }

    fun getMatchupByWinLossRecord(winLossRecord: WinLossRecord): Pool? {
        return matchesMap.get(winLossRecord)
    }

    override fun toString(): String {
        return "Round $number"
    }
}