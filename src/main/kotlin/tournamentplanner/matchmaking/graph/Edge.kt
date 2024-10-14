package io.wongaz.tournamentplanner.matchmaking.graph

class Edge(
    val node1: INode,
    val node2: INode,
    val heuristicOverride: Int = 1) {

    fun getOtherNode(incomingNode: INode): INode? {
        if (node1 == incomingNode){
            return node2
        }
        if (node2 == incomingNode){
            return node1
        }
        return null
    }

    override fun hashCode(): Int {
        return node1.hashCode() + node2.hashCode()
    }
}