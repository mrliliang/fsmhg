package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledVertex;

public class PointPattern extends Pattern {

    int label;

    public PointPattern(int label) {
        super(null, null);
        this.label = label;
    }

    public int label() {
        return label;
    }

}
