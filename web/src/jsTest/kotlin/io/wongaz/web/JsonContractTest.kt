package io.wongaz.web

import io.wongaz.api.ApiError
import io.wongaz.api.CreateRunRequest
import io.wongaz.api.DatasetDto
import io.wongaz.api.SavedRunDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonContractTest {
    @Test
    fun requestUsesDatasetIdAndIntegerIterations() {
        assertEquals(
            """{"datasetId":"worlds2024","iterations":10000}""",
            Json.encodeToString(CreateRunRequest("worlds2024", 10000))
        )
    }

    @Test
    fun decodesSavedSnapshotUsingSharedContract() {
        val payload = """
            {
                "summary": {
                    "id": "00000000-0000-0000-0000-000000000001",
                    "datasetId": "worlds2024",
                    "iterations": 10000,
                    "startedAt": "2026-09-28T07:00:00Z",
                    "completedAt": "2026-09-28T07:00:01Z"
                },
                "results": [{
                    "teamSignature": "T1-LCK-4",
                    "teamName": "T1",
                    "region": "LCK",
                    "seed": 4,
                    "category": "MAJOR",
                    "elo": 1850,
                    "qualifications": 7500
                }]
            }
        """.trimIndent()
        val saved = Json.decodeFromString<SavedRunDto>(payload)
        assertEquals(snapshot("00000000-0000-0000-0000-000000000001"), saved)
        assertEquals(75.0, saved.results.single().percentage(saved.summary.iterations))
        assertEquals(saved, Json.decodeFromString<SavedRunDto>(Json.encodeToString(saved)))
    }

    @Test
    fun decodesDatasetListAndSafeError() {
        assertEquals(
            listOf(DatasetDto("worlds2024", "Worlds 2024")),
            Json.decodeFromString<List<DatasetDto>>("""[{"id":"worlds2024","label":"Worlds 2024"}]""")
        )
        assertEquals(
            "Database unavailable.",
            Json.decodeFromString<ApiError>("""{"message":"Database unavailable."}""").message
        )
    }

    @Test
    fun formattingIsDeterministicWithoutBrowserGlobals() {
        assertEquals("10,000", grouped(10000))
        assertEquals("1,000,000", grouped(1000000))
        assertEquals("0.00%", percentage(0, 100))
        assertEquals("33.33%", percentage(1, 3))
        assertEquals("100.00%", percentage(10000, 10000))
        assertEquals("—", percentage(0, 0))
    }
}
