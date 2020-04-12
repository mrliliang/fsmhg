package com.liang.fsmhg.graph;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class HistoricalGraph {

    private Map<Integer, DynamicVertex> vMap;
    private Map<Integer, AdjEdges> adjLists;

    long start;
    long end;

    public HistoricalGraph() {
        vMap = new HashMap<>();
        adjLists = new HashMap<>();
    }

    public Snapshot snapshot() {
        return new Snapshot(this, end++);
    }

    protected DynamicVertex vertex(int id) {
        DynamicVertex v = vMap.get(id);
        if (v == null) {
            v = new DynamicVertex(id);
            vMap.put(id, v);
        }
        return v;
    }

    protected DynamicEdge edge(int from, int to) {
        DynamicVertex vfrom = vMap.get(from);
        if (vfrom == null) {
            throw new RuntimeException("No vertex " + from + ".");
        }
        DynamicVertex vto = vMap.get(to);
        if (vto == null) {
            throw new RuntimeException("No vertex " + to + ".");
        }
        AdjEdges adjEdges = adjLists.get(from);
        DynamicEdge e = (DynamicEdge)adjEdges.edgeTo(to);
        if (e == null) {
            e = new DynamicEdge(vfrom, vto);
            adjEdges.add(e);
        }
        return e;
    }

}
