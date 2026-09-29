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
    val completedAt: String,
    val tournamentCount: Int = 0
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
data class TournamentDto(
    val iteration: Int,
    val rounds: List<TournamentRoundDto>,
    val qualified: List<TeamStandingDto>,
    val eliminated: List<TeamStandingDto>
)

@Serializable
data class TournamentRoundDto(val number: Int, val groups: List<PairingGroupDto>)

@Serializable
data class PairingGroupDto(val wins: Int, val losses: Int, val matches: List<PairingDto>)

@Serializable
data class PairingDto(
    val team1: String,
    val team2: String,
    val team1Wins: Int,
    val team2Wins: Int,
    val firstTo: Int
)

@Serializable
data class TeamStandingDto(val teamSignature: String, val wins: Int, val losses: Int)

@Serializable
data class ApiError(val message: String)
