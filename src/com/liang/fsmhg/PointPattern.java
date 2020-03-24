package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledVertex;

import java.util.ArrayList;
import java.util.List;

public class PointPattern extends Pattern {

    int label;

    public PointPattern(int label) {
        super();
        this.label = label;
    }

    public int label() {
        return label;
    }

    public void addPoint(long graphId, LabeledVertex v) {
        addEmbedding(graphId, new Embedding(v, null));
    }

}
