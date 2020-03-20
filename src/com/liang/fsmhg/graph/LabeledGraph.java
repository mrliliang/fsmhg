package com.liang.fsmhg.graph;

import java.util.List;

public abstract class LabeledGraph<V extends Vertex, E extends AEdge<V>> extends AGraph<V, E> {

    public LabeledGraph(long id) {
        this(id, null, null);
    }

    public LabeledGraph(long id, List vertices, List edges) {
        super(id, vertices, edges);
    }

    public abstract int vLabel(V v);

    public abstract int eLabel(E e);

    public abstract V addVertex(int id, int label);

    public abstract E addEdge(int from, int to, int eLabel);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        List<V> vList = vertices();
        for (V v : vList) {
            builder.append(v.id()).append('\n');
        }

        for (V v : vList) {
            for (E e : adjEdges(v.id())) {
                builder.append(e.from().id())
                        .append(" ")
                        .append(e.to().id())
                        .append('\n');
            }
        }
        return builder.toString();
    }
}
