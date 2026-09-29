package io.wongaz.persistence

import io.wongaz.runs.RunConfigurationException

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
