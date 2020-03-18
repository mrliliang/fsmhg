package com.liang.fsmhg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticGraph extends Graph<StaticVertex, StaticEdge> {

    public StaticGraph(long id) {
        super(id);
    }

    public StaticGraph(long id, List<StaticVertex> vertices, List<StaticEdge> edges) {
        super(id, vertices, edges);
    }

    public StaticVertex addVertex(int id, int label) {
        StaticVertex v = new StaticVertex(id, label);
        addVertex(v);
        return v;
    }

    public StaticEdge addEdge(StaticVertex from, StaticVertex to, int eLabel) {
        StaticEdge e = new StaticEdge(from, to, eLabel);
        addEdge(e);
        return e;
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
