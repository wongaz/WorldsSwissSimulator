package io.wongaz.runs

import java.util.UUID

class RunConfigurationException(message: String) : IllegalArgumentException(message)

/**
 * Blocking operations: HTTP handlers must use a background dispatcher.
 * Only successfully persisted, completed runs are returned by run().
 */
interface RunService {
    val datasets: List<DatasetOption>
    fun listRuns(): List<RunSummary>
    fun getRun(id: UUID): SavedRun?
    fun run(datasetId: String, iterations: Int): SavedRun
}
