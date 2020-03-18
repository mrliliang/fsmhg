package com.liang.fsmhg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Graph<V extends Vertex, E extends Edge> {
    private long graphId;

    private Map<Integer, V> vertices;
    private Map<Integer, AdjEdges<E>> adjLists;

    public Graph(long id) {
        this.graphId = id;
        vertices = new HashMap<>();
        adjLists = new HashMap<>();
    }

    public Graph(long id, List<V> vertices, List<E> edges) {
        this(id);

        for (V v : vertices) {
            addVertex(v);
        }

        for (E e : edges) {
            addEdge(e);
        }
    }

    public long graphId() {
        return graphId;
    }

    public void addVertex(V v) {
        vertices.put(v.id(), v);
        adjLists.put(v.id(), new AdjEdges<>());
    }

    public void addEdge(E e) {
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

    public abstract int vLabel(V v);

    public abstract int eLabel(E e);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (V v : vertices.values()) {
            builder.append(vLabel(v)).append('\n');
        }
        for (AdjEdges<E> adj : adjLists.values()) {
            for (E e : adj) {
                builder.append(eLabel(e)).append('\n');
            }
        }
        return builder.toString();
    }
}
