package com.liang.fsmhg;

public class Edge<V extends Vertex> {

    private V from;
    private V to;

    public Edge(V from, V to) {
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
