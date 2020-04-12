package com.liang.fsmhg.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LabeledGraph implements Comparable<LabeledGraph> {
    private long graphId;

    private Map<Integer, LabeledVertex> vertices;
    private Map<Integer, AdjEdges> adjLists;

    public LabeledGraph(long id) {
        this(id, null, null);
    }

    public LabeledGraph(long id, List<? extends LabeledVertex> vertices, List<? extends LabeledEdge> edges) {
        this.graphId = id;
        this.vertices = new HashMap<>();
        this.adjLists = new HashMap<>();

        if (vertices != null) {
            for (LabeledVertex v : vertices) {
                addVertex(v);
            }
        }

        if (edges != null) {
            for (LabeledEdge e : edges) {
                addEdge(e);
            }
        }
    }

    public long graphId() {
        return this.graphId;
    }

    public abstract int vLabel(LabeledVertex v);

    public abstract int eLabel(LabeledEdge e);

    public abstract LabeledVertex addVertex(int id, int label);

    public abstract LabeledEdge addEdge(int from, int to, int eLabel);

    protected void addVertex(LabeledVertex v) {
        vertices.put(v.id(), v);
        adjLists.put(v.id(), new AdjEdges());
    }

    protected void addEdge(LabeledEdge e) {
        adjLists.get(e.from().id()).add(e);
    }

    public List<LabeledVertex> vertices() {
        return new ArrayList<>(vertices.values());
    }

    public LabeledVertex vertex(int id) {
        return vertices.get(id);
    }

    public LabeledEdge edge(int from, int to) {
        return adjLists.get(from).edgeTo(to);
    }

    public AdjEdges adjEdges(int vertexId) {
        return adjLists.get(vertexId);
    }

    public int vSize() {
        return vertices.size();
    }

    public int eSize() {
        int size = 0;
        for (AdjEdges adjEdges : adjLists.values()) {
            size += adjEdges.size();
        }
        return size / 2;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        List<LabeledVertex> vList = vertices();
        for (LabeledVertex v : vList) {
            builder.append(v.id()).append('\n');
        }

        for (LabeledVertex v : vList) {
            for (LabeledEdge e : adjEdges(v.id())) {
                builder.append(e.from().id())
                        .append(" ")
                        .append(e.to().id())
                        .append('\n');
            }
        }
        return builder.toString();
    }

    @Override
    public int compareTo(LabeledGraph other) {
        if (this.graphId < other.graphId) {
            return -1;
        }
        if (this.graphId > other.graphId) {
            return 1;
        }
        return 10;
    }
}
