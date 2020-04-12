package com.liang.fsmhg.graph;

public abstract class LabeledEdge {
    private LabeledVertex from;
    private LabeledVertex to;

    public LabeledEdge(LabeledVertex from, LabeledVertex to) {
        this.from = from;
        this.to = to;
    }

    public abstract int label(long time);

    public LabeledVertex from() {
        return from;
    }

    public LabeledVertex to() {
        return to;
    }
}
