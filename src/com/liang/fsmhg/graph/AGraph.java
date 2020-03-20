package com.liang.fsmhg.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AGraph<V extends Vertex, E extends AEdge<V>> {
    private long graphId;

    private Map<Integer, V> vertices;
    private Map<Integer, AdjEdges<E>> adjLists;

    public AGraph(long id) {
        this.graphId = id;
        vertices = new HashMap<>();
        adjLists = new HashMap<>();
    }

    public AGraph(long id, List<V> vertices, List<E> edges) {
        this(id);

        if (vertices != null) {
            for (V v : vertices) {
                addVertex(v);
            }
        }

        if (edges != null) {
            for (E e : edges) {
                addEdge(e);
            }
        }
    }

    public long graphId() {
        return graphId;
    }

    protected void addVertex(V v) {
        vertices.put(v.id(), v);
        adjLists.put(v.id(), new AdjEdges<>());
    }

    protected void addEdge(E e) {
        adjLists.get(e.from().id()).add(e);
    }

    public List<V> vertices() {
        return new ArrayList<>(vertices.values());
    }

    public V vertex(int id) {
        return vertices.get(id);
    }

    public E edge(int from, int to) {
        return adjLists.get(from).edgeTo(to);
    }

    public AdjEdges<E> adjEdges(int vertexId) {
        return adjLists.get(vertexId);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (AdjEdges<E> adj : adjLists.values()) {
            for (E e : adj) {
                builder.append(e.from().id())
                        .append(" ")
                        .append(e.to().id())
                        .append('\n');
            }
        }
        return builder.toString();
    }
}
