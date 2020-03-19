package com.liang.fsmhg.graph;

public class HistoricalGraph {

    private VertexPool vPool;
    private EdgePool ePool;

    long start;
    long end;

    public Snapshot snapshot() {
        return new Snapshot(this, end++);
    }

}
