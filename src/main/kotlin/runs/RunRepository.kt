package io.wongaz.runs

import io.wongaz.api.TournamentDto
import java.util.UUID

interface RunRepository {
    fun initialize()
    /** Consumes tournament details once, atomically with the run and team snapshots. */
    fun save(run: SavedRun, tournaments: Sequence<TournamentDto> = emptySequence())
    fun listRuns(limit: Int = 100): List<RunSummary>
    fun findRun(id: UUID): SavedRun?
    fun findTournament(id: UUID, iteration: Int): TournamentDto?
}
