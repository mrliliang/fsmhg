package com.liang.fsmhg.graph;

import java.util.List;

public class StaticGraph extends LabeledGraph {

    public StaticGraph() {
        this(0);
    }

    public StaticGraph(long id) {
        this(id, null, null);
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
        StaticVertex vfrom = (StaticVertex) vertex(from);
        if (vfrom == null) {
            throw new RuntimeException("No vertex " + from + ".");
        }
        StaticVertex vto = (StaticVertex) vertex(to);
        if (vto == null) {
            throw new RuntimeException("No vertex " + to + ".");
        }
        StaticEdge e = new StaticEdge(vfrom, vto, eLabel);
        adjEdges(from).add(e);

        return e;
    }

    @Override
    public int vLabel(LabeledVertex v) {
        return v.label(graphId());
    }

    @Override
    public int eLabel(LabeledEdge e) {
        return e.label(graphId());
    }

    @Override
    public AdjEdges adjEdges(int vertexId) {
        return super.adjEdges(vertexId);
    }

    @Override
    public List<LabeledVertex> vertices() {
        return super.vertices();
    }
}
