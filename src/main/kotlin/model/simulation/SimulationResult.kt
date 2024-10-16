package io.wongaz.model.simulation

import io.wongaz.model.core.Team

data class SimulationResult(
    private val team: Team,
    private val iterations: Int){

    var qualification = 0.0

    fun getPercentage():Double{
        return (qualification/iterations) * 100
    }

    fun addQualification(){
        this.qualification += 1
    }

    fun makeSimpleResultsLine(): String {
        return "${this.team.teamSignature} - ${this.qualification} - ${this.getPercentage()}"
    }
}
