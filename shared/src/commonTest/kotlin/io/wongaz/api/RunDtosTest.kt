package io.wongaz.api

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RunDtosTest {
    @Test
    fun tournamentRoundTripPreservesGroupsScoresAndTerminalRecords() {
        val tournament = TournamentDto(
            1_000_000,
            listOf(TournamentRoundDto(5, listOf(PairingGroupDto(
                2, 2, listOf(PairingDto("T1", "GEN", 2, 1, 2))
            )))),
            listOf(TeamStandingDto("T1", 3, 2)),
            listOf(TeamStandingDto("GEN", 2, 3))
        )
        assertEquals(tournament, Json.decodeFromString<TournamentDto>(Json.encodeToString(tournament)))
        val summary = RunSummaryDto("id", "worlds2024.yml", 10, "start", "end", 10)
        assertEquals(summary, Json.decodeFromString<RunSummaryDto>(Json.encodeToString(summary)))
        val legacy = """{"id":"id","datasetId":"worlds2024.yml","iterations":10,"startedAt":"start","completedAt":"end"}"""
        assertEquals(0, Json.decodeFromString<RunSummaryDto>(legacy).tournamentCount)
    }

    @Test
    fun savedRunPreservesTeamSnapshotAcrossJsonRoundTrip() {
        val run = SavedRunDto(
            RunSummaryDto(
                "85786f67-c4a1-4b44-aeee-c67ed8b493bd", "worlds2024.yml", 10,
                "2026-09-28T00:00:00Z", "2026-09-28T00:00:01.123456Z"
            ),
            listOf(TeamResultDto("T1", "Team \"One\"", "LCK", 1, "EASTERN", 1500, 7))
        )

        assertEquals(run, Json.decodeFromString<SavedRunDto>(Json.encodeToString(run)))
        assertEquals(70.0, run.results.single().percentage(run.summary.iterations))
    }

    @Test
    fun createRunUsesNamedDatasetAndIntegerIterations() {
        assertEquals(
            CreateRunRequest("worlds2024.yml", 100),
            Json.decodeFromString<CreateRunRequest>("""{"datasetId":"worlds2024.yml","iterations":100}""")
        )
        assertFailsWith<SerializationException> {
            Json.decodeFromString<CreateRunRequest>("""{"datasetId":"worlds2024.yml","iterations":1.5}""")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString<CreateRunRequest>("""{"datasetId":"worlds2024.yml"}""")
        }
    }
}
