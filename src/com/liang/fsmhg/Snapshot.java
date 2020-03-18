package com.liang.fsmhg;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Snapshot extends Graph<DynamicVertex, DynamicEdge> {

    public Snapshot(long id) {
        super(id);
    }

    public Snapshot(long id, List<DynamicVertex> vertices, List<DynamicEdge> edges) {
        super(id, vertices, edges);
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
