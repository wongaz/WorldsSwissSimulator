package io.wongaz.runs

import io.wongaz.api.PairingDto
import io.wongaz.api.PairingGroupDto
import io.wongaz.api.TeamStandingDto
import io.wongaz.api.TournamentDto
import io.wongaz.api.TournamentRoundDto
import io.wongaz.tournamentplanner.scheduler.SwissFormatScheduler

internal fun SwissFormatScheduler.snapshot(iteration: Int): TournamentDto {
    val records = mutableMapOf<String, TeamStandingDto>()
    val rounds = getRounds().map { round ->
        TournamentRoundDto(round.number, round.getPools().map { (record, pool) ->
            val wins = record.getWins()
            val losses = record.getLoss()
            PairingGroupDto(wins, losses, pool.matches.map { match ->
                val winner = checkNotNull(match.getWinner()).teamSignature
                val loser = checkNotNull(match.getLoser()).teamSignature
                records[winner] = TeamStandingDto(winner, wins + 1, losses)
                records[loser] = TeamStandingDto(loser, wins, losses + 1)
                PairingDto(
                    match.team1.teamSignature, match.team2.teamSignature,
                    match.getTeam1Wins(), match.getTeam2Wins(), match.firstTo
                )
            })
        }.sortedByDescending { it.wins })
    }
    return TournamentDto(
        iteration, rounds,
        getQualifiedTeams().map { records.getValue(it.teamSignature) },
        getEliminatedTeams().map { records.getValue(it.teamSignature) }
    )
}
