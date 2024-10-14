package io.wongaz.loader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.wongaz.model.LoadedTeams
import io.wongaz.model.Team
import java.io.InputStream

class TeamLoaderManager() {
    fun getTeamsFromStream(stream: InputStream): List<Team> {
        val kotlinModule = KotlinModule.Builder()
            .build()
        val mapper = ObjectMapper(YAMLFactory()).registerModule(kotlinModule)
        return mapper.readValue<LoadedTeams>(stream).teams
    }
}