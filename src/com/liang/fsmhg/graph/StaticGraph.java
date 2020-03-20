package com.liang.fsmhg.graph;

import java.util.List;

public class StaticGraph extends LabeledGraph<StaticVertex, StaticEdge> {

    public StaticGraph(long id) {
        super(id);
    }

    public StaticGraph(long id, List<StaticVertex> vertices, List<StaticEdge> edges) {
        super(id, vertices, edges);
    }

    @Override
    public StaticVertex addVertex(int id, int label) {
        StaticVertex v = new StaticVertex(id, label);
        addVertex(v);
        return v;
    }

    @Override
    public StaticEdge addEdge(int from, int to, int eLabel) {
        StaticVertex vfrom = vertex(from);
        if (vfrom == null) {
            throw new RuntimeException("No vertex " + from + ".");
        }
        StaticVertex vto = vertex(to);
        if (vto == null) {
            throw new RuntimeException("No vertex " + to + ".");
        }
        StaticEdge e = new StaticEdge(vfrom, vto, eLabel);
        adjEdges(from).add(e);

        return null;
    }

    @Override
    public int vLabel(StaticVertex v) {
        return v.label();
    }

    @Override
    public int eLabel(StaticEdge e) {
        return e.label();
    }

}
