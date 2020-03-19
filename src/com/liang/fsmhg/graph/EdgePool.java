package com.liang.fsmhg.graph;

import java.util.HashMap;
import java.util.Map;

public class EdgePool {

    private Map<Integer, AdjEdges<DynamicEdge>> adjLists;

    public EdgePool() {
        adjLists = new HashMap<>();
    }

    public DynamicEdge edge(int from, int to) {
        AdjEdges<DynamicEdge> edges = adjLists.get(from);
        if (edges == null) {
            return null;
        }
        return edges.edgeTo(to);
    }

    public DynamicEdge addEdge(DynamicEdge e) {
        AdjEdges<DynamicEdge> edges = adjLists.get(e.from().id());
        if (edges == null) {
            edges = new AdjEdges();
            adjLists.put(e.from().id(), edges);
        }
        edges.add(e);
        return e;
    }

}
