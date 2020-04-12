package com.liang.fsmhg.graph;


import java.util.List;

public class Snapshot extends LabeledGraph {
    private HistoricalGraph hg;

    protected Snapshot(HistoricalGraph hg, long time) {
        this(hg, time, null, null);
    }

    private Snapshot(HistoricalGraph hg, long time, List<LabeledVertex> vertices, List<LabeledEdge> edges) {
        super(time, vertices, edges);
        this.hg = hg;
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
    public LabeledVertex addVertex(int id, int label) {
        DynamicVertex v = hg.vertex(id);
        v.addLabel(graphId(), label);
        addVertex(v);
        return v;
    }

    @Override
    public LabeledEdge addEdge(int from, int to, int eLabel) {
        DynamicEdge e = hg.edge(from, to);
        e.addLabel(graphId(), eLabel);
        addEdge(e);
        return e;
    }

    @Override
    public List<LabeledVertex> vertices() {
        return super.vertices();
    }

    @Override
    public AdjEdges adjEdges(int vertexId) {
        return super.adjEdges(vertexId);
    }
}
