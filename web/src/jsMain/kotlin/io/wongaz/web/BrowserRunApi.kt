package io.wongaz.web

import io.wongaz.api.ApiError
import io.wongaz.api.CreateRunRequest
import io.wongaz.api.DatasetDto
import io.wongaz.api.RunSummaryDto
import io.wongaz.api.SavedRunDto
import io.wongaz.api.TournamentDto
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Headers
import org.w3c.fetch.NO_STORE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit

class BrowserRunApi : HttpRunApi {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun datasets(): List<DatasetDto> = get("/api/datasets")

    override suspend fun history(): List<RunSummaryDto> = get("/api/runs")

    override suspend fun savedRun(id: String): SavedRunDto {
        require(id.matches(Regex("[a-zA-Z0-9-]+"))) { "Invalid saved run identifier." }
        return get("/api/runs/$id")
    }

    override suspend fun createRun(request: CreateRunRequest): SavedRunDto {
        val headers = Headers()
        headers.set("Content-Type", "application/json")
        headers.set("Accept", "application/json")
        return json.decodeFromString(
            send("/api/runs", RequestInit(method = "POST", headers = headers, body = json.encodeToString(request)))
        )
    }

    override suspend fun tournament(id: String, iteration: Int): TournamentDto {
        require(id.matches(Regex("[a-zA-Z0-9-]+"))) { "Invalid saved run identifier." }
        require(iteration in 1..1_000_000) { "Invalid tournament number." }
        return get("/api/runs/$id/tournaments/$iteration")
    }

    private suspend inline fun <reified T> get(path: String): T =
        json.decodeFromString(send(path, RequestInit(method = "GET", cache = RequestCache.NO_STORE)))

    private suspend fun send(path: String, options: RequestInit): String {
        val response = window.fetch(path, options).await()
        val body = response.text().await()
        if (!response.ok) {
            val message = runCatching { json.decodeFromString<ApiError>(body).message }
                .getOrNull()?.takeIf { it.isNotBlank() }
                ?: when (response.status.toInt()) {
                    400 -> "Check the dataset and iteration count."
                    404 -> "This saved run was not found. Refresh history."
                    409 -> "Another simulation is running. Wait, then refresh history."
                    503 -> "The database is unavailable or needs server-side configuration. Refresh after setup."
                    else -> "The server could not complete this request. Please try again later."
                }
            throw RunApiException(message, response.status.toInt())
        }
        return body
    }
}
