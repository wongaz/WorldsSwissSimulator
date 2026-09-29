package io.wongaz.persistence

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import io.wongaz.runs.RunConfigurationException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseConfigTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `local dotenv password fills missing environment and preserves connection defaults`() {
        Files.writeString(directory.resolve(".env"), "SIM_DB_PASSWORD=local-test-password\n")

        val config = DatabaseConfig.fromLocalEnvironment(emptyMap(), directory)

        assertEquals("local-test-password", config.password)
        assertEquals("worlds_swiss", config.user)
        assertEquals("jdbc:postgresql://localhost:5432/worlds_swiss", config.url)
    }

    @Test
    fun `dotenv accepts comments quoted values and Windows line endings`() {
        Files.writeString(
            directory.resolve(".env"),
            "# Local database\r\nSIM_DB_URL=jdbc:postgresql://localhost:5433/test\r\n" +
                "SIM_DB_USER=\"local-user\"\r\nSIM_DB_PASSWORD=\"test # password=with spaces\"\r\n" +
                "SUPERSET_SECRET_KEY=unrelated-test-value\r\n"
        )

        val config = DatabaseConfig.fromLocalEnvironment(emptyMap(), directory)

        assertEquals("jdbc:postgresql://localhost:5433/test", config.url)
        assertEquals("local-user", config.user)
        assertEquals("test # password=with spaces", config.password)
    }

    @Test
    fun `explicit environment takes precedence including invalid blank values`() {
        Files.writeString(
            directory.resolve(".env"),
            "SIM_DB_URL=jdbc:postgresql://localhost/file\nSIM_DB_USER=file-user\nSIM_DB_PASSWORD=file-password\n"
        )
        val environment = mapOf(
            "SIM_DB_URL" to "jdbc:postgresql://localhost/environment",
            "SIM_DB_USER" to "environment-user",
            "SIM_DB_PASSWORD" to "environment-password"
        )
        assertEquals(DatabaseConfig.fromEnvironment(environment), DatabaseConfig.fromLocalEnvironment(environment, directory))
        for (key in environment.keys) {
            assertFailsWith<RunConfigurationException> {
                DatabaseConfig.fromLocalEnvironment(environment + (key to ""), directory)
            }
        }
    }

    @Test
    fun `missing dotenv is optional but password is still required`() {
        val environment = mapOf("SIM_DB_PASSWORD" to "environment-password")
        assertEquals(DatabaseConfig.fromEnvironment(environment), DatabaseConfig.fromLocalEnvironment(environment, directory))
        assertFailsWith<RunConfigurationException> { DatabaseConfig.fromLocalEnvironment(emptyMap(), directory) }
        Files.writeString(directory.resolve(".env"), "SUPERSET_SECRET_KEY=unrelated-test-value\n")
        assertFailsWith<RunConfigurationException> { DatabaseConfig.fromLocalEnvironment(emptyMap(), directory) }
    }

    @Test
    fun `malformed dotenv fails explicitly without disclosing its content`() {
        val secret = "test-secret-not-for-logs"
        Files.writeString(directory.resolve(".env"), "INVALID $secret\n")

        val failure = assertFailsWith<RunConfigurationException> {
            DatabaseConfig.fromLocalEnvironment(emptyMap(), directory)
        }

        assertTrue(failure.message.orEmpty().contains(".env"))
        assertFalse(failure.stackTraceToString().contains(secret))
    }

    @Test
    fun `explicit environment supplies PostgreSQL configuration`() {
        val config = DatabaseConfig.fromEnvironment(
            mapOf(
                "SIM_DB_URL" to "jdbc:postgresql://database:5432/simulator",
                "SIM_DB_USER" to "test-user",
                "SIM_DB_PASSWORD" to "test-password"
            )
        )

        assertEquals("jdbc:postgresql://database:5432/simulator", config.url)
        assertEquals("test-user", config.user)
        assertEquals("test-password", config.password)
    }

    @Test
    fun `local URL and username have defaults but password must be explicit`() {
        val config = DatabaseConfig.fromEnvironment(mapOf("SIM_DB_PASSWORD" to "test-password"))

        assertEquals("jdbc:postgresql://localhost:5432/worlds_swiss", config.url)
        assertEquals("worlds_swiss", config.user)
        val failure = assertFailsWith<IllegalArgumentException> {
            DatabaseConfig.fromEnvironment(emptyMap())
        }
        assertTrue(failure.message.orEmpty().contains("SIM_DB_PASSWORD"))
    }

    @Test
    fun `blank values are rejected instead of falling back silently`() {
        val valid = mapOf(
            "SIM_DB_URL" to "jdbc:postgresql://database:5432/simulator",
            "SIM_DB_USER" to "test-user",
            "SIM_DB_PASSWORD" to "test-password"
        )
        for (key in valid.keys) {
            for (blank in listOf("", "   ")) {
                val failure = assertFailsWith<IllegalArgumentException> {
                    DatabaseConfig.fromEnvironment(valid + (key to blank))
                }
                assertTrue(failure.message.orEmpty().contains(key))
            }
        }
    }

    @Test
    fun `string representation redacts passwords including JDBC URL credentials`() {
        val secret = "test-secret-not-for-logs"
        val config = DatabaseConfig(
            "jdbc:postgresql://localhost/simulator?password=$secret",
            "test-user", secret
        )

        assertFalse(config.toString().contains(secret))
        assertFalse(config.toString().contains(config.url))
    }
}
