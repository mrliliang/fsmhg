package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledVertex;

import java.util.ArrayList;
import java.util.List;

public class Embedding {

    private Embedding parent;
    private LabeledVertex v;

    public Embedding(LabeledVertex v, Embedding parent) {
        this.v = v;
        this.parent = parent;
    }

    public LabeledVertex vertex() {
        return v;
    }

    public List<LabeledVertex> vertices() {
        return vertices(1);
    }

    private List<LabeledVertex> vertices(int count) {
        if (this.parent == null) {
            List<LabeledVertex> vertices = new ArrayList<>(count);
            vertices.add(v);
            return vertices;
        }
        List<LabeledVertex> vertices = this.parent.vertices(count + 1);
        vertices.add(v);
        return vertices;
    }

}
