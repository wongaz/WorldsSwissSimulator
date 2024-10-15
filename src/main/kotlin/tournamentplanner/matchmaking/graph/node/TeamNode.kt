package io.wongaz.tournamentplanner.matchmaking.graph.node

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.component.INode

data class TeamNode(val team:Team): INode {
}