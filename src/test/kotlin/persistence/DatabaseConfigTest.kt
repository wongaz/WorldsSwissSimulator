package io.wongaz.persistence

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseConfigTest {
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
