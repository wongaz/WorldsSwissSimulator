package io.wongaz.persistence

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvException
import io.wongaz.runs.RunConfigurationException
import java.nio.file.Files
import java.nio.file.Path

data class DatabaseConfig(val url: String, val user: String, val password: String) {
    init {
        if (url.isBlank()) throw RunConfigurationException("SIM_DB_URL must not be blank.")
        if (user.isBlank()) throw RunConfigurationException("SIM_DB_USER must not be blank.")
        if (password.isBlank()) {
            throw RunConfigurationException(
                "Set SIM_DB_PASSWORD to the PostgreSQL password before using run history."
            )
        }
    }

    override fun toString(): String = "DatabaseConfig(user=$user, credentials=<redacted>)"

    companion object {
        fun fromLocalEnvironment(
            environment: Map<String, String> = System.getenv(),
            directory: Path = Path.of(".")
        ): DatabaseConfig {
            val fileValues = if (Files.notExists(directory.resolve(".env"))) {
                emptyMap()
            } else {
                try {
                    Dotenv.configure().directory(directory.toAbsolutePath().toString()).load()
                        .entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
                        .associate { it.key to it.value }
                } catch (_: DotenvException) {
                    // Parser messages can contain raw credential lines.
                    throw RunConfigurationException("The local .env file could not be read. Check its syntax and permissions.")
                }
            }
            return fromEnvironment(fileValues + environment)
        }

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): DatabaseConfig =
            DatabaseConfig(
                url = environment["SIM_DB_URL"] ?: "jdbc:postgresql://localhost:5432/worlds_swiss",
                user = environment["SIM_DB_USER"] ?: "worlds_swiss",
                password = environment["SIM_DB_PASSWORD"]
                    ?: throw RunConfigurationException(
                        "Set SIM_DB_PASSWORD to the PostgreSQL password before using run history."
                    )
            )
    }
}
