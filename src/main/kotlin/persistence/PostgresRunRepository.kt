package io.wongaz.persistence

import io.wongaz.api.TournamentDto
import io.wongaz.model.core.Category
import io.wongaz.runs.RunRepository
import io.wongaz.runs.RunSummary
import io.wongaz.runs.SavedRun
import io.wongaz.runs.TeamRunResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties
import java.util.UUID

class PostgresRunRepository(private val config: DatabaseConfig) : RunRepository {
    override fun initialize() {
        val schema = requireNotNull(javaClass.getResourceAsStream("/db/schema.sql")) {
            "The PostgreSQL schema resource is missing from the application."
        }.bufferedReader().use { it.readText() }
        transaction { connection ->
            connection.createStatement().use { statement ->
                schema.split(';').map(String::trim).filter(String::isNotEmpty).forEach {
                    statement.execute(it)
                }
            }
        }
    }

    override fun save(run: SavedRun, tournaments: Sequence<TournamentDto>) {
        transaction { connection ->
            connection.prepareStatement(
                """
                INSERT INTO simulation_runs (id, dataset_id, iterations, started_at, completed_at, tournament_count)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setObject(1, run.summary.id)
                statement.setString(2, run.summary.datasetId)
                statement.setInt(3, run.summary.iterations)
                statement.setObject(4, run.summary.startedAt.atOffset(ZoneOffset.UTC))
                statement.setObject(5, run.summary.completedAt.atOffset(ZoneOffset.UTC))
                statement.setInt(6, run.summary.tournamentCount)
                statement.executeUpdate()
            }

            connection.prepareStatement(
                """
                INSERT INTO team_run_results
                    (run_id, result_order, team_signature, team_name, region, seed, category, elo, qualifications)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                run.results.forEachIndexed { index, result ->
                    require(result.qualifications <= run.summary.iterations) {
                        "Team qualifications must not exceed the run's iteration count."
                    }
                    statement.setObject(1, run.summary.id)
                    statement.setInt(2, index)
                    statement.setString(3, result.teamSignature)
                    statement.setString(4, result.teamName)
                    statement.setString(5, result.region)
                    statement.setInt(6, result.seed)
                    statement.setString(7, result.category.name)
                    statement.setInt(8, result.elo)
                    statement.setInt(9, result.qualifications)
                    statement.addBatch()
                }
                statement.executeBatch()
            }

            connection.prepareStatement(
                "INSERT INTO tournament_results (run_id, iteration, detail) VALUES (?, ?, ?::jsonb)"
            ).use { statement ->
                var count = 0
                tournaments.forEach { tournament ->
                    require(tournament.iteration in 1..run.summary.tournamentCount) {
                        "Tournament number is outside the run's recorded range."
                    }
                    statement.setObject(1, run.summary.id)
                    statement.setInt(2, tournament.iteration)
                    statement.setString(3, Json.encodeToString(tournament))
                    statement.addBatch()
                    count++
                    if (count % 100 == 0) {
                        statement.executeBatch()
                        statement.clearBatch()
                    }
                }
                if (count % 100 != 0) statement.executeBatch()
                require(count == run.summary.tournamentCount) { "Every tournament must be saved." }
            }
        }
    }

    override fun listRuns(limit: Int): List<RunSummary> {
        require(limit > 0) { "The history limit must be positive." }
        return connection().use { connection ->
            connection.prepareStatement(
                """
                SELECT id, dataset_id, iterations, started_at, completed_at, tournament_count
                FROM simulation_runs
                ORDER BY completed_at DESC, id DESC
                LIMIT ?
                """.trimIndent()
            ).use { statement ->
                statement.setInt(1, limit)
                statement.executeQuery().use { rows ->
                    buildList {
                        while (rows.next()) add(rows.toSummary())
                    }
                }
            }
        }
    }

    override fun findRun(id: UUID): SavedRun? = connection().use { connection ->
        val summary = connection.prepareStatement(
            """
            SELECT id, dataset_id, iterations, started_at, completed_at, tournament_count
            FROM simulation_runs
            WHERE id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.toSummary() else null
            }
        } ?: return@use null

        val results = connection.prepareStatement(
            """
            SELECT team_signature, team_name, region, seed, category, elo, qualifications
            FROM team_run_results
            WHERE run_id = ?
            ORDER BY result_order
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) {
                        add(
                            TeamRunResult(
                                teamSignature = rows.getString("team_signature"),
                                teamName = rows.getString("team_name"),
                                region = rows.getString("region"),
                                seed = rows.getInt("seed"),
                                category = Category.valueOf(rows.getString("category")),
                                elo = rows.getInt("elo"),
                                qualifications = rows.getInt("qualifications")
                            )
                        )
                    }
                }
            }
        }
        SavedRun(summary, results)
    }

    override fun findTournament(id: UUID, iteration: Int): TournamentDto? = connection().use { connection ->
        connection.prepareStatement(
            "SELECT detail FROM tournament_results WHERE run_id = ? AND iteration = ?"
        ).use { statement ->
            statement.setObject(1, id)
            statement.setInt(2, iteration)
            statement.executeQuery().use { rows ->
                if (rows.next()) Json.decodeFromString<TournamentDto>(rows.getString("detail")) else null
            }
        }
    }

    private fun connection(): Connection {
        val properties = Properties().apply {
            setProperty("user", config.user)
            setProperty("password", config.password)
            setProperty("connectTimeout", "5")
            setProperty("loginTimeout", "5")
            setProperty("socketTimeout", "15")
            setProperty("tcpKeepAlive", "true")
        }
        return DriverManager.getConnection(config.url, properties)
    }

    private fun <T> transaction(block: (Connection) -> T): T = connection().use { connection ->
        connection.autoCommit = false
        try {
            val result = block(connection)
            connection.commit()
            result
        } catch (failure: Exception) {
            // Streaming details can also fail on file reads or decoding; never commit a partial run.
            rollback(connection, failure)
            throw failure
        }
    }

    private fun rollback(connection: Connection, failure: Exception) {
        try {
            connection.rollback()
        } catch (rollbackFailure: SQLException) {
            failure.addSuppressed(rollbackFailure)
        }
    }

    private fun ResultSet.toSummary(): RunSummary = RunSummary(
        id = getObject("id", UUID::class.java),
        datasetId = getString("dataset_id"),
        iterations = getInt("iterations"),
        startedAt = getObject("started_at", OffsetDateTime::class.java).toInstant(),
        completedAt = getObject("completed_at", OffsetDateTime::class.java).toInstant(),
        tournamentCount = getInt("tournament_count")
    )
}
