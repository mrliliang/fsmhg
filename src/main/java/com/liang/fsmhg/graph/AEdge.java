package com.liang.fsmhg.graph;

import java.util.Comparator;

public abstract class AEdge<V extends Vertex> {

    private V from;
    private V to;

    public AEdge(V from, V to) {
        this.from = from;
        this.to = to;
    }

    public V from() {
        return from;
    }

    public V to() {
        return to;
    }
}
