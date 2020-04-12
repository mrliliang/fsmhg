package com.liang.fsmhg.graph;

import com.liang.fsmhg.graph.Vertex;

public abstract class LabeledVertex {
    private int id;

    public LabeledVertex(int id) {
        this.id = id;
    }

    public abstract int label(long graphId);

    public int id() {
        return id;
    }
}
