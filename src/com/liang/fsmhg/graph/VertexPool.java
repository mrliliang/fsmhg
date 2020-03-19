package com.liang.fsmhg.graph;

import com.liang.fsmhg.graph.DynamicVertex;

import java.util.HashMap;
import java.util.Map;

public class VertexPool {

    private Map<Integer, DynamicVertex> vMap;

    public VertexPool() {
        vMap = new HashMap<>();
    }

    public DynamicVertex vertex(int id) {
        return vMap.get(id);
    }

    public DynamicVertex add(int id) {
        DynamicVertex v = new DynamicVertex(id);
        vMap.put(v.id(), v);
        return v;
    }

}
