package io.wongaz.tournamentplanner.matchmaking.graph.matching

import org.jgrapht.Graph
import org.jgrapht.GraphTests
import org.jgrapht.Graphs
import org.jgrapht.alg.interfaces.MatchingAlgorithm
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching
import org.jgrapht.alg.interfaces.MatchingAlgorithm.MatchingImpl
import kotlin.random.Random

/*
* (C) Copyright 2017-2023, by Joris Kinable and Contributors.
*
* JGraphT : a free Java graph-theory library
*
* See the CONTRIBUTORS.md file distributed with this work for additional
* information regarding copyright ownership.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the
* GNU Lesser General Public License v2.1 or later
* which is available at
* http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
*
* SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
*/


/**
 * The greedy algorithm for computing a maximum cardinality matching. The algorithm can run in two
 * modes: sorted or unsorted. When unsorted, the matching is obtained by iterating through the edges
 * and adding an edge if it doesn't conflict with the edges already in the matching. When sorted,
 * the edges are first sorted by the sum of degrees of their endpoints. After that, the algorithm
 * proceeds in the same manner. Running this algorithm in sorted mode can sometimes produce better
 * results, albeit at the cost of some additional computational overhead.
 *
 *
 * Independent of the mode, the resulting matching is maximal, and is therefore guaranteed to
 * contain at least half of the edges that a maximum cardinality matching has ($\frac{1}{2}$
 * approximation). Runtime complexity: $O(m)$ when the edges are not sorted, $O(m + m \log n)$
 * otherwise, where $n$ is the number of vertices, and $m$ the number of edges.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Joris Kinable
</E></V> */
class RandomizedGreedyMaximumCardinalityMatching<V, E>
    (graph: Graph<V, E>, private val sort: Boolean = false, private val seed: Random) : MatchingAlgorithm<V, E> {
    private val graph: Graph<V, E> = GraphTests.requireUndirected(graph)

    /**
     * Get a matching that is a $\frac{1}{2}$-approximation of the maximum cardinality matching.
     *
     * @return a matching
     */
    override fun getMatching(): Matching<V, E> {
        val matched: MutableSet<V> = HashSet()
        val edges: MutableSet<E> = LinkedHashSet()
        var cost = 0.0
        if (sort) {
            // sort edges in increasing order of the total degree of their endpoints
            var allEdges: List<E> = ArrayList(graph.edgeSet())
            allEdges = allEdges.sortedWith(EdgeDegreeComparator())

            for (e in allEdges) {
                val v = graph.getEdgeSource(e)
                val w = graph.getEdgeTarget(e)
                if (v != w && !matched.contains(v) && !matched.contains(w)) {
                    edges.add(e)
                    matched.add(v)
                    matched.add(w)
                    cost += graph.getEdgeWeight(e)
                }
            }
        } else {
            val shuffledVerices = this.graph.vertexSet().shuffled()
            for (v in shuffledVerices) {
                if (matched.contains(v)) continue

                val shuffledEdges = graph.edgesOf(v).shuffled()
                for (e in shuffledEdges) {
                    val w = Graphs.getOppositeVertex(graph, e, v)
                    if (v != w && !matched.contains(w)) {
                        edges.add(e)
                        matched.add(v)
                        matched.add(w)
                        cost += graph.getEdgeWeight(e)
                        break
                    }
                }
            }
        }
        return MatchingImpl(graph, edges, cost)
    }

    private inner class EdgeDegreeComparator()
        : Comparator<E> {
        override fun compare(e1: E, e2: E): Int {
            val degreeE1 =
                graph.degreeOf(graph.getEdgeSource(e1)) + graph.degreeOf(graph.getEdgeTarget(e1))
            val degreeE2 =
                graph.degreeOf(graph.getEdgeSource(e2)) + graph.degreeOf(graph.getEdgeTarget(e2))
            return Integer.compare(degreeE1, degreeE2)
        }
    }
}
