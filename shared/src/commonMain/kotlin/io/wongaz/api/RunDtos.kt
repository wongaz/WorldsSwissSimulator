package io.wongaz.api

import kotlinx.serialization.Serializable

@Serializable
data class DatasetDto(val id: String, val label: String)

@Serializable
data class CreateRunRequest(val datasetId: String, val iterations: Int)

@Serializable
data class RunSummaryDto(
    val id: String,
    val datasetId: String,
    val iterations: Int,
    val startedAt: String,
    val completedAt: String
)

@Serializable
data class TeamResultDto(
    val teamSignature: String,
    val teamName: String,
    val region: String,
    val seed: Int,
    val category: String,
    val elo: Int,
    val qualifications: Int
) {
    fun percentage(iterations: Int): Double = qualifications * 100.0 / iterations
}

@Serializable
data class SavedRunDto(val summary: RunSummaryDto, val results: List<TeamResultDto>)

@Serializable
data class ApiError(val message: String)
