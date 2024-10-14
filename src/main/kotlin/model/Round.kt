package io.wongaz.model

class Round(private val number: Int) {
    private val matchesMap = mutableMapOf<WinLossRecord, Pool>()

    fun addPool(winLossRecord: WinLossRecord, pool: Pool ) {
        matchesMap.put(winLossRecord, pool)
    }
}