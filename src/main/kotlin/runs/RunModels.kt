package io.wongaz.runs

import io.wongaz.model.core.Category
import java.time.Instant
import java.util.UUID

data class DatasetOption(val id: String, val label: String)

data class RunSummary(
    val id: UUID,
    val datasetId: String,
    val iterations: Int,
    val startedAt: Instant,
    val completedAt: Instant
)

data class TeamRunResult(
    val teamSignature: String,
    val teamName: String,
    val region: String,
    val seed: Int,
    val category: Category,
    val elo: Int,
    val qualifications: Int
) {
    fun percentage(iterations: Int): Double = qualifications * 100.0 / iterations
}

data class SavedRun(val summary: RunSummary, val results: List<TeamRunResult>)
