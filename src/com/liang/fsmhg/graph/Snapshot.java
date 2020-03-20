package com.liang.fsmhg.graph;


import java.util.List;

public class Snapshot extends LabeledGraph<DynamicVertex, DynamicEdge> {
    private HistoricalGraph hg;

    protected Snapshot(HistoricalGraph hg, long time) {
        this(hg, time, null, null);
    }

    private Snapshot(HistoricalGraph hg, long time, List<DynamicVertex> vertices, List<DynamicEdge> edges) {
        super(time, vertices, edges);
        this.hg = hg;
    }

    @Override
    public int vLabel(DynamicVertex v) {
        return v.label(graphId());
    }

    @Override
    public int eLabel(DynamicEdge e) {
        return e.label(graphId());
    }

}
