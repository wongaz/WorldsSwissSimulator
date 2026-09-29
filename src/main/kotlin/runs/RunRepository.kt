package io.wongaz.runs

import java.util.UUID

interface RunRepository {
    fun initialize()
    fun save(run: SavedRun)
    fun listRuns(limit: Int = 100): List<RunSummary>
    fun findRun(id: UUID): SavedRun?
}
